from zmlpsdk import AssetProcessor, Argument
from zmlpsdk.analysis import LabelDetectionAnalysis
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_clarifai_app

models = [
    'apparel-model',
    'food-model',
    'general-model',
    'moderation-model',
    'nsfw-model',
    'textures-and-patterns-model',
    'travel-model',
    'wedding-model',
]


class ClarifaiLabelDetectionProcessor(AssetProcessor):
    namespace = 'clarifai-label'

    def __init_(self):
        super(ClarifaiLabelDetectionProcessor, self).__init__()
        for model in models:
            self.add_arg(Argument(model, "boolean", required=False,
                                  toolTip="Enable the {} model".format(model)))
        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        for model_name in models:
            if self.arg_value(model_name):
                model = getattr(self.clarifai.public_models, model_name.replace("-", "_"))
                response = model.predict_by_filename(p_path)
                labels = response['outputs'][0]['data'].get('concepts')
                if not labels:
                    continue

                analysis = LabelDetectionAnalysis()
                for label in labels:
                    analysis.add_label_and_score(label['name'], label['value'])

                asset.add_analysis("-".join([self.namespace, model_name]), analysis)
