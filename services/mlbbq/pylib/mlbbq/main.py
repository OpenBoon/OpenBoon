#!/usr/bin/env python3

import argparse
import logging
import os
import importlib
import random
import string

from flask import Flask, jsonify, g
from gevent.pywsgi import WSGIServer

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

logger = logging.getLogger('mlbbq')


@app.before_request
def before_request():
    request_id = ''.join(random.choices(string.ascii_uppercase + string.digits, k=24))
    g.request_id = request_id


def main():
    parser = argparse.ArgumentParser(prog='zmld')
    parser.add_argument("-p", "--port", help="The port to listen on",
                        default=os.environ.get("BOONAI_PORT", "8282"))
    parser.add_argument("-v", "--verbose", help="Debug logging",
                        action="store_true")

    args = parser.parse_args()

    flask_log = logging.getLogger('werkzeug')
    flask_log.disabled = True
    app.logger.disabled = True

    if os.environ.get("BOONAI_DEBUG") or args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    setup_endpoints()
    logger.info("Listening on port {}".format(args.port))
    server = WSGIServer(('0.0.0.0', int(args.port)), app, log=None)
    server.serve_forever()


def setup_endpoints():
    modules = ["similarity", "face", "modules"]
    for mod in modules:
        logger.info(f"setting up endpoints for {mod}")
        imported = importlib.import_module(f"mlbbq.{mod}")
        imported.setup_endpoints(app)


@app.route('/healthcheck', methods=['GET'])
def get_heath_check():
    return jsonify({'healthy': True})
