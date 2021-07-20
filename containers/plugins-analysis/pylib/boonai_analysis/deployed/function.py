import re

import requests

from boonsdk.util import to_json
from boonflow import Argument, FileTypes
from boonflow.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis
from ..custom.base import CustomModelProcessor


class BoonFunctionProcessor(CustomModelProcessor):
    file_types = FileTypes.images | FileTypes.documents

    def __init__(self):
        super(BoonFunctionProcessor, self).__init__()
        self.add_arg(Argument("endpoint", "str", required=True))
        self.add_arg(Argument("model", "str", default="model1"))
        self.endpoint = None

    def init(self):
        """Init constructor """
        # get model by model id
        self.load_app_model()
        self.endpoint = self.arg_value('endpoint')

    def process(self, frame):
        self.process_asset(frame)

    def predict(self, asset):
        headers = {
            'Content-Type': 'application/json'
        }
        rsp = requests.post(self.endpoint, data=to_json(asset), headers=headers)
        rsp.raise_for_status()
        return rsp.json()

    def process_asset(self, frame):
        asset = frame.asset
        result = self.predict(asset)
        for analysis in result.get('analysis', []):

            analysis_type = analysis.get('type')
            if not analysis_type:
                raise ValueError('There is no analysis type')

            ns = self.get_analysis_ns(analysis)
            if analysis_type == 'labels':
                labels = LabelDetectionAnalysis()
                for pred in analysis.get('predictions', []):
                    labels.add_label_and_score(pred['label'], pred['score'])
                asset.add_analysis(ns, labels)
            elif analysis_type == 'content':
                content = ContentDetectionAnalysis()
                content.add_content(analysis['content'])
                asset.add_analysis(ns, content)
            else:
                raise ValueError(f'Unsupported analysis type: {analysis_type}')

        for k, v in result.get("custom-fields", {}).items():
            if not self.validate_name(k):
                raise ValueError(f'The customn field name is not allowed: {k}')
            asset.set_attr(f'custom.{k}', v)

    def get_analysis_ns(self, analysis):
        section = analysis.get("section")
        if section:
            if not self.validate_name(section):
                raise ValueError("The analysis section name is not alpha-num")
            return f'{self.app_model.module_name}-{section}'
        else:
            return self.app_model.module_name

    def validate_name(self, name):
        return re.fullmatch('[A-Za-z0-9_]+', name, re.IGNORECASE) is not None