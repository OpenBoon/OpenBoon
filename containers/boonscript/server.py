#!/usr/bin/env python3
import logging
import json

import flask
from gevent.pywsgi import WSGIServer
from boonsdk import Asset

import script

logger = logging.getLogger('boonai')
logging.basicConfig(level=logging.INFO)

app = flask.Flask(__name__)


@app.route('/', methods=['POST'])
def endpoint():
    asset = Asset(json.loads(flask.request.data))
    return flask.jsonify(script.process(asset))


if __name__ == '__main__':
    logger.info('Listening on port 8080')
    server = WSGIServer(('0.0.0.0', 8080), app, log=None)
    server.serve_forever()
