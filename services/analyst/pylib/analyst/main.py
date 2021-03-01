#!/usr/bin/env python3

import argparse
import logging
import os

from flask import Flask, jsonify, request, abort
from gevent.pywsgi import WSGIServer

import analyst.service as service

app = Flask(__name__)


def main():
    parser = argparse.ArgumentParser(prog='analyst')
    parser.add_argument('-a', '--archivist', help='The URL of the Archivist server.',
                        default=os.environ.get('BOONAI_SERVER', 'http://archivist:8080'))
    parser.add_argument('-p', '--port', help='The port to listen on',
                        default=os.environ.get('ANALYST_PORT', '5000'))
    parser.add_argument('-c', '--credentials',
                        help='The path to a file storing archivist cluster credentials')
    parser.add_argument('-l', '--poll', default=5,
                        help='Seconds to wait before polling for a new task. 0 to disable')
    parser.add_argument('-g', '--ping', default=30,
                        help='Seconds to wait between each ping, 0 to disable')
    parser.add_argument('-v', '--verbose', action='store_true', default=False,
                        help='Enable verbose logging')
    args = parser.parse_args()

    if os.environ.get('BOONAI_DEBUG') or args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    flask_log = logging.getLogger('werkzeug')
    flask_log.disabled = True
    app.logger.disabled = True

    api = service.ServiceComponents(args)
    setup_routes(api)

    print('Listening on port {}'.format(args.port))
    server = WSGIServer(('0.0.0.0', int(args.port)), app, log=None)
    server.serve_forever()


def setup_routes(api):
    @app.route('/')
    def root():
        return 'Zorroa Analyst'

    @app.route('/kill/<tid>', methods=['DELETE'])
    def kill(tid):
        if not request.json:
            abort(400)
        reason = request.json.get('reason', 'unknown kill reason from %s' % request.remote_addr)
        new_state = request.json.get('newState')
        return jsonify({'op': 'kill', 'status': api.executor.kill_task(tid, new_state, reason)})

    @app.route('/info', methods=['GET'])
    def info():
        return jsonify({'version': service.get_sdk_version()})

    @app.route('/prestop', methods=['GET'])
    def shutdown():
        return jsonify(api.executor.start_shutdown())
