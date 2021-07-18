#!/usr/bin/env python3
import logging
import json

import flask
from gevent.pywsgi import WSGIServer
from boonsdk import Asset
from boonsdk.util import to_json

import function

logger = logging.getLogger('boonai')
logging.basicConfig(level=logging.INFO)

app = flask.Flask(__name__)


@app.route('/', methods=['POST'])
def endpoint():
    try:
        asset = Asset(json.loads(flask.request.data))
        result = function.process(asset)
        if result:
            return flask.Response(to_json(result), mimetype='application/json')
    except Exception as e:
        logger.exception('Failed to process request: {}'.format(e))
        return str(e), 400
    return flask.jsonify({})


if __name__ == '__main__':
    logger.info('Listening on port 8080')
    server = WSGIServer(('0.0.0.0', 8080), app, log=None)
    server.serve_forever()
