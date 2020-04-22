from zmlpsdk import AssetProcessor
from zmlpsdk.proxy import get_proxy_level_path

from .util import get_clarifai_app


class ClarifaiPredictGeneralProcessor(AssetProcessor):
    namespace = 'clarifai-predict-general'

    def __init_(self):
        super(ClarifaiPredictGeneralProcessor, self).__init__()
        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        model = self.clarifai.public_models.general_model
        response = model.predict_by_filename(p_path)
        labels = response['outputs'][0]['data']['concepts']

        result = [
            {'label': label['name'],
             'score': round(label['value'], 3)} for label in labels]

        asset.add_analysis(self.namespace, {
            'predictions': result,
            'count': len(result),
            'type': 'labels'
        })
