from clarifai.rest import ClarifaiApp

import zmlp
from zmlp import ZmlpException
from zmlpsdk import ZmlpEnv, ZmlpFatalProcessorException


def get_clarifai_app():
    app = zmlp.app_from_env()
    jobid = ZmlpEnv.get_job_id()
    try:
        creds = app.client.get('/api/v1/jobs/{}/_credentials/CLARIFAI'.format(jobid))
        return ClarifaiApp(api_key=creds['apikey'])
    except ZmlpException as e:
        raise ZmlpFatalProcessorException("Job is missing Clarifai credentials", e)
