#!/usr/bin/env python3
import json
import logging
import random
import string
import sys
import traceback
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
                return custom_error("process() must return a FunctionResponse object")
            else:
                return flask.Response(to_json(result), mimetype='application/json')
    except Exception as e:
        logger.exception('Failed to process request: {}'.format(e))
        return custom_error(str(e), sys.exc_info()[2])

    return flask.jsonify({})


def custom_error(message, exec_traceback=None):
    err_id = str(uuid.uuid4())
    status_code = 551
    logging.warning(f'{message} - error_id = {err_id}')
    payload = {
        'errorId': err_id,
        'code': status_code,
        'message': message,
        'path': '/',
        'exception': 'BoonFunctionException'
    }

    if exec_traceback:
        trace_limit = 10
        trace = traceback.extract_tb(exec_traceback)
        if len(trace) > trace_limit:
            trace = trace[-trace_limit:]

        stack_trace_for_payload = []
        for ste in trace:
            stack_trace_for_payload.append({
                "filename": ste[0],
                "lineno": ste[1],
                "name": ste[2],
                "line": ste[3]
            })
        payload["stackTrace"] = stack_trace_for_payload

    return flask.Response(to_json(payload), status=status_code, mimetype='application/json')
