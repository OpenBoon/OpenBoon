import json
import logging
import io

import flask

import boonsdk
import boondocks.process as boonproc
from boondocks.reactor import Reactor
from boonflow import Frame, ImageInputStream, file_storage
from boonflow.env import app_instance
from boonsdk import Asset
from .auth import check_write_access

logger = logging.getLogger('mlbbq-modules')


class BBQExecutor:
    """
    A Zps executor specially for MLBBQ.
    """

    def __init__(self, pipeline, image=None):
        self.exec = boonproc.ProcessorExecutor(reactor=Reactor(self))
        self.pipeline = pipeline
        self.image = image
        self.error = False

    def write(self, event):
        if event['type'] == 'error':
            logger.warning("EVENT {}".format(event))
            self.error = True

    def execute(self):
        wrappers = []
        for ref in self.pipeline.get("execute", []):
            wrapper = self.exec.get_processor_wrapper(ref, {})
            wrapper.init()
            if not wrapper.instance:
                raise RuntimeError('Unable to initialize processor: {}'.format(ref))
            wrapper.instance.fatal_errors = True
            wrappers.append(wrapper)

        results = []
        for asset in self.pipeline.get("assets", []):
            frame = Frame(Asset(asset))
            frame.image = self.image
            for wrapper in wrappers:
                logger.info('Applying {} to {}'.format(wrapper.ref['className'], frame.asset.id))
                wrapper.process(frame, force=True)
                if self.error:
                    break
            results.append(frame.asset.for_json())
        return results

    def code(self):
        """
        Returns the proper response code for pipeline operation.  200 or 400.

        Returns:
            int: Response code
        """
        if self.error:
            return 412
        else:
            return 200


def setup_endpoints(app):

    @app.route('/ml/v1/pipelines/apply-modules-to-asset', methods=['PUT', 'POST'])
    @app.route('/ml/v1/modules/apply-to-asset', methods=['PUT', 'POST'])
    def apply_to_asset():
        check_write_access()

        try:
            app = app_instance()
            req = json.loads(flask.request.data)

            # Get the ZpsScript necessary for processing the request.
            script = app.client.post('/api/v3/pipelines/resolver/_build_script', req)

            exec = BBQExecutor(script)
            results = exec.execute()

            if not results:
                flask.abort(400, description='No Assets processed')

            asset = results[0]
            if req.get('index'):
                logger.info('indexing assets {}'.format(asset['id']))
                body = {'assets': {asset['id']: asset['document']}}
                app.client.put('/api/v3/assets/_batch_index', body)

            return flask.Response(boonsdk.to_json(asset),
                                  mimetype='application/json', status=exec.code())

        except Exception as e:
            logger.exception('Failed to execute pipeline: {}'.format(e))
            flask.abort(500, description='Unexpected server side exception')
        finally:
            file_storage.cache.clear_request_cache()

    @app.route('/ml/v1/modules/apply-to-file', methods=['POST'])
    def apply_to_file():
        check_write_access()

        app = app_instance()
        modules = flask.request.args.get('modules', None)
        if not modules:
            flask.abort(400, description='Must supply "modules" in query string')

        try:

            # Get the ZpsScript necessary for processing the request.
            req = {
                'assetId': None,
                'modules': modules.split(',')
            }
            script = app.client.post('/api/v3/pipelines/resolver/_build_script', req)

            # Load the image.
            data = ImageInputStream(io.BytesIO(flask.request.data))

            # Execute pipeline
            exec = BBQExecutor(script, image=data)
            results = exec.execute()
            if not results:
                flask.abort(400, description='File not processed')

            return flask.Response(boonsdk.to_json(results[0]),
                                  mimetype='application/json', status=exec.code())

        except Exception as e:
            logger.exception('Failed to execute pipeline: {}'.format(e))
            flask.abort(500, description='Unexpected server side exception')
        finally:
            file_storage.cache.clear_request_cache()
