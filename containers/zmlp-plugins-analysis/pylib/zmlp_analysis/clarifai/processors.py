from clarifai.rest import ClarifaiApp

import zmlp
from zmlp import ZmlpException
from zmlpsdk import AssetProcessor, ZmlpEnv, ZmlpFatalProcessorException
from zmlpsdk.proxy import get_proxy_level_path


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


def get_clarifai_app():
    app = zmlp.app_from_env()
    jobid = ZmlpEnv.get_job_id()
    try:
        creds = app.client.get('/api/v1/jobs/{}/_credentials/CLARIFAI'.format(jobid))
        return ClarifaiApp(api_key=creds['apikey'])
    except ZmlpException as e:
        raise ZmlpFatalProcessorException("Job is missing Clarifai credentials", e)
