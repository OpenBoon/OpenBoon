import json
import logging

import flask

import boondocks.process as boonproc
from boondocks.reactor import Reactor
from boonflow import Frame, app_instance
from boonsdk import Asset
from .auth import check_write_access

logger = logging.getLogger('mlbbq-pipeline')


class BBQExecutor:
    """
    A Zps executor specially for MLBBQ.
    """

    def __init__(self, pipeline):
        self.exec = boonproc.ProcessorExecutor(reactor=Reactor(self))
        self.pipeline = pipeline

    def write(self, event):
        if event['type'] == 'error':
            logger.warning("EVENT {}".format(event))

    def execute(self):
        wrappers = []
        for ref in self.pipeline.get("execute", []):
            wrapper = self.exec.get_processor_wrapper(ref, {})
            wrapper.init()
            wrappers.append(wrapper)

        results = []
        for asset in self.pipeline.get("assets", []):
            frame = Frame(Asset(asset))
            for wrapper in wrappers:
                logger.info('Applying {} to {}'.format(wrapper.ref['className'], frame.asset.id))
                wrapper.process(frame)
            results.append(frame.asset.for_json())
        return results


def setup_endpoints(app):
    @app.route('/ml/v1/apply-modules', methods=['PUT', 'POST'])
    def execute_pipeline():
        check_write_access()

        try:
            app = app_instance()
            req = json.loads(flask.request.data)

            # Get the ZpsScript necessary for processing the request.
            script = app.client.post('/api/v3/pipelines/resolver/_apply_modules_to_asset', req)

            exec = BBQExecutor(script)
            result = exec.execute()

            if req.get('index'):
                logger.info('indexing assets {}'.format(result[0]['id']))
                body = {'assets': {result[0]['id']: result[0]['document']}}
                app.client.put('/api/v3/assets/_batch_index', body)

            return flask.jsonify(result[0])

        except Exception:
            logger.exception('Failed to execute pipeline')
            flask.abort(500, description='Unexpected server side exception')
