#!/usr/bin/env python3

import argparse
import logging
import os
import importlib

from flask import Flask, jsonify
from gevent.pywsgi import WSGIServer

app = Flask(__name__)
logger = logging.getLogger('mlbbq')


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
    modules = ["similarity", "face", "pipeline"]
    for mod in modules:
        logger.info(f"setting up endpoints for {mod}")
        imported = importlib.import_module(f"mlbbq.{mod}")
        imported.setup_endpoints(app)


@app.route('/healthcheck', methods=['GET'])
def get_heath_check():
    return jsonify({'healthy': True})
