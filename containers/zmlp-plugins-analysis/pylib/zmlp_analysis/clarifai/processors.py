from clarifai.rest import ClarifaiApp

import zmlp
from zmlp.exception import ZmlpException
from zmlpsdk import AssetProcessor, ZmlpEnv, ZmlpFatalProcessorException
from zmlpsdk.proxy import get_proxy_level_path


class ClarifaiPredictProcessor(AssetProcessor):
    namespace = 'clarifai.predict'

    def __init_(self):
        super(ClarifaiPredictProcessor, self).__init__()
        self.clarifai = None

    def init(self):
        self.clarifai = get_clarifai_app()

    def process(self, frame):
        asset = frame.asset
        p_path = get_proxy_level_path(asset, 1)

        model = self.clarifai.public_models.general_model
        response = model.predict_by_filename(p_path)
        labels = response['outputs'][0]['data']['concepts']
        model = response['outputs'][0]['model']['name']

        result = [{'label': label['name'], 'score': label['value']} for label in labels]
        asset.add_analysis(self.namespace, {'model': model, 'labels': result})


def get_clarifai_app():
    app = zmlp.app_from_env()
    jobid = ZmlpEnv.get_job_id()
    try:
        creds = app.client.get('/api/v1/jobs/{}/_credentials/CLARIFAI'.format(jobid))
        return ClarifaiApp(api_key=creds['apikey'])
    except ZmlpException as e:
        raise ZmlpFatalProcessorException("Job is missing Clarifai credentials", e)
