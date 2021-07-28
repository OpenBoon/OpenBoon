import re

import backoff
import requests

from boonflow import Argument, FileTypes
from boonflow.analysis import LabelDetectionAnalysis, ContentDetectionAnalysis
from boonsdk.util import to_json
from ..custom.base import CustomModelProcessor


class BoonFunctionProcessor(CustomModelProcessor):
    file_types = FileTypes.all

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
            'Content-Type': 'application/json',
            'Authorization': self.app.client.sign_request()
        }
        rsp = requests.post(self.endpoint, data=to_json(asset), headers=headers, timeout=30)
        rsp.raise_for_status()
        return rsp.json()

    def process_asset(self, frame):
        asset = frame.asset
        self.logger.info(f'Calling BoonFunction {self.endpoint}')
        result = self.predict(asset)
        self.logger.info('BoonFunction responded')

        for section, analysis in result.get('analysis', {}).items():

            analysis_type = analysis.get('type')
            if not analysis_type:
                raise ValueError('There is no analysis type')

            ns = self.get_analysis_ns(section)
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

    def get_analysis_ns(self, section):
        if section == "__MAIN__":
            return self.app_model.module_name
        else:
            if not self.validate_name(section):
                raise ValueError("The analysis section name is not alpha-num")
            return f'{self.app_model.module_name}-{section}'

    def validate_name(self, name):
        return re.fullmatch('[A-Za-z0-9_\\-]+', name, re.IGNORECASE) is not None
