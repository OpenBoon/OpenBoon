#!/usr/bin/env python3

import argparse
import logging
import os

from flask import Flask, jsonify, request, abort
from gevent.pywsgi import WSGIServer

import analyst.components as components

app = Flask(__name__)


def main():
    parser = argparse.ArgumentParser(prog='analyst')
    parser.add_argument("-a", "--archivist", help="The URL of the Archivist server.",
                        default=os.environ.get("ZORROA_ARCHIVIST_URL", "http://archivist:8080"))
    parser.add_argument("-p", "--port", help="The port to listen on",
                        default=os.environ.get("ZORROA_ANALYST_PORT", "5000"))
    parser.add_argument("-l", "--poll", default=5,
                        help="Seconds to wait before polling for a new task. 0 to disable")
    parser.add_argument("-g", "--ping", default=30,
                        help="Seconds to wait between each ping, 0 to disable")
    parser.add_argument("-v", "--verbose", action="store_true", default=False,
                        help="Enable verbose logging")
    args = parser.parse_args()

    if os.environ.get("ZMLP_DEBUG") or args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    api = components.ApiComponents(args)
    setup_routes(api)

    print("Listening on port {}".format(args.port))
    server = WSGIServer(('0.0.0.0', int(args.port)), app)
    server.serve_forever()


def setup_routes(api):
    @app.route('/')
    def root():
        return 'Zorroa Analyst'

    @app.route('/kill/<tid>', methods=['DELETE'])
    def kill(tid):
        if not request.json:
            abort(400)
        reason = request.json.get("reason", "unknown kill reason from %s" % request.remote_addr)
        new_state = request.json.get("newState")
        return jsonify({"op": "kill", "status": api.executor.kill_task(tid, new_state, reason)})

    @app.route('/info', methods=['GET'])
    def info():
        return jsonify({"version": components.get_sdk_version()})
