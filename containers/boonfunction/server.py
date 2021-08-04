#!/usr/bin/env python3
import json
import logging
import random
import string
import uuid

import flask

import boonsdk.func
from boonsdk import Asset
from boonsdk.util import to_json
from function import function

logger = logging.getLogger('boonfunc')
logging.basicConfig(level=logging.DEBUG)

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
            if not isinstance(result, boonsdk.func.FunctionResponse):
                return custom_error("process() must return a FunctionResponse object", 417)
            else:
                return flask.Response(to_json(result), mimetype='application/json')
    except Exception as e:
        logger.exception('Failed to process request: {}'.format(e))
        return custom_error(str(e), 412)

    return flask.jsonify({})


def custom_error(message, status_code):
    err_id = str(uuid.uuid4())
    logging.warning(f'{message} - error_id = {err_id}')
    struct = {
        'errorId': err_id,
        'code': status_code,
        'message': message
    }
    return flask.Response(to_json(struct), status=status_code, mimetype='application/json')
