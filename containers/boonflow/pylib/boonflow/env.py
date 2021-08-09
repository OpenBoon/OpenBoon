import os
import logging

from flask import request

from boonsdk import BoonClient, BoonApp, app_from_env

logger = logging.getLogger("boonflow.env")


def app_instance():
    """
    Create an app instance by first attempting to detect a flask request, then
    falling back on the environment.  The BOONFLOW_IN_FLASK environment variable
    must be set for the Flask integration to work.

    Returns:
        BoonApp: The configured BoonApp
    """
    if os.environ.get("BOONFLOW_IN_FLASK"):
        app = BoonApp(apikey=None)
        app.client = FlaskBoonClient(app.client)
        return app
    else:
        return app_from_env()


class FlaskBoonClient(BoonClient):
    """
    A BoonAI client that automatically uses an Authorization header
    from a Flask request to make additional requests.
    """
    endpoint_map = {
        'prod': 'https://api.boonai.app',
        'qa': 'https://qa.api.boonai.app',
        'dev': 'https://dev.api.boonai.app',
    }

    def __init__(self, client):
        super(FlaskBoonClient, self).__init__(None, None)

    def get_server(self):
        if os.environ.get("BOONAI_SERVER"):
            return os.environ.get("BOONAI_SERVER")
        else:
            env_name = os.environ.get("BOONAI_ENV") or 'prod'
            endpoint = self.endpoint_map.get(env_name)
            logger.info(f'Selected BoonAI endpoint {endpoint} / env: {env_name}')
            return endpoint

    def sign_request(self):
        token = request.headers.get("Authorization")
        return token

    def headers(self, content_type="application/json"):
        headers = super().headers()
        headers['X-BoonAI-Experimental-XXX'] = "8E5B551A8F51477489B1CC0FFD65C1C5"
        return headers


class BoonEnv:
    """
    Static methods for obtaining environment variables available when running
    within an analysis container.
    """

    @staticmethod
    def get_job_id():
        """
        Return the Boon AI Job id from the environment.

        Returns:
            str: The Boon AI task Id.
        """
        return os.environ.get("BOONAI_JOB_ID")

    @staticmethod
    def get_task_id():
        """
        Return the Boon AI Task id from the environment.

        Returns:
            str: The Boon AI task Id.
        """
        return os.environ.get("BOONAI_TASK_ID")

    @staticmethod
    def get_project_id():
        """
        Return the Boon AI project id from the environment.

        Returns:
            str: The Boon AI project Id.
        """
        return os.environ.get("BOONAI_PROJECT_ID")

    @staticmethod
    def get_datasource_id():
        """
        Return the Boon AI DataSource id from the environment.  The DataSource ID
        may or may not exist.

        Returns:
            str: The Boon AI DataSource Id or None
        """
        return os.environ.get("BOONAI_DATASOURCE_ID")

    @staticmethod
    def get_available_credentials_types():
        """
        Get a list of the available credentials types available to this job.

        Returns:
            list: list of credentials types.
        """
        return os.environ.get("BOONAI_CREDENTIALS_TYPES", "").split(",")
