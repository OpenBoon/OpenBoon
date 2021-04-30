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

        results = {}
        for asset in self.pipeline.get("assets", []):
            frame = Frame(Asset(asset))
            for wrapper in wrappers:
                wrapper.process(frame)
            results[frame.asset.id] = frame.asset.for_json().get('document')
        return results


def setup_endpoints(app):
    @app.route('/ml/v1/apply-modules', methods=['PUT', 'POST'])
    def execute_pipeline():
        check_write_access()

        app = app_instance()
        req = json.loads(flask.request.data)

        # Get the ZpsScript necessary for processing the request.
        script = app.client.post('/api/v3/pipelines/resolver/_apply_modules_script', req)

        try:
            exec = BBQExecutor(script)
            result = exec.execute()

            if req.get('index'):
                logger.info("indexing assets {}".format(result.keys()))
                body = {'assets': result}
                app.client.put('/api/v3/assets/_batch_index', body)

            return flask.jsonify({'asset': result[req['assetId']]})

        except Exception as e:
            print(e)
            logger.error('Failed to execute pipeline', e)
            flask.abort(500, description='Unexpected server side exception')
