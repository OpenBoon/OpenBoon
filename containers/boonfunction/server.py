#!/usr/bin/env python3
import logging
import json
import random
import string

import flask
from gevent.pywsgi import WSGIServer
from boonsdk import Asset
from boonsdk.util import to_json
from boonflow import file_storage

from function import function

logger = logging.getLogger('boonfunc')
logging.basicConfig(level=logging.INFO)

app = flask.Flask(__name__)


@app.before_request
def before_request():
    request_id = ''.join(random.choices(string.ascii_uppercase + string.digits, k=24))
    flask.g.request_id = request_id


@app.route('/', methods=['POST'])
def endpoint():
    try:
        asset = Asset(json.loads(flask.request.data))
        result = function.process(asset)
        if result:
            return flask.Response(to_json(result), mimetype='application/json')
    except Exception as e:
        logger.exception('Failed to process request: {}'.format(e))
        return str(e), 412
    finally:
        file_storage.cache.clear_request_cache()
    return flask.jsonify({})


if __name__ == '__main__':
    logger.info('Listening on port 8080')
    server = WSGIServer(('0.0.0.0', 8080), app, log=None)
    server.serve_forever()
