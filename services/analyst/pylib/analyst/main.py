#!/usr/bin/env python3

import argparse
import logging
import os
import socket
import subprocess

from flask import Flask, jsonify, request, abort, send_file
from pathlib2 import Path

import analyst.components as components

app = Flask(__name__)
scanner = components.ProcessorScanner()


def main():
    logging.basicConfig(level=logging.INFO)

    parser = argparse.ArgumentParser(prog='analyst')
    parser.add_argument("-a", "--archivist", help="The URL of the Archivist server.",
                        default=os.environ.get("ZORROA_ARCHIVIST_URL", "http://archivist:8080"))
    parser.add_argument("-p", "--port", help="The port to listen on",
                        default=os.environ.get("ZORROA_ANALYST_PORT", "5000"))
    parser.add_argument("-l", "--poll", default=5,
                        help="Seconds to wait before polling for a new task. 0 to disable")
    parser.add_argument("-g", "--ping", default=30,
                        help="Seconds to wait between each ping, 0 to disable",)
    args = parser.parse_args()

    api = components.ApiComponents(args)
    setup_routes(api)

    create_ssl_files()
    app.run(host='0.0.0.0', port=int(args.port),
            ssl_context=('certs/analyst.cert', 'certs/analyst.key'))


def create_ssl_files():
    """Creates self signed ssl files that use the analyst's ip and domain as valid hosts."""
    hostname = socket.gethostname()
    ip = socket.gethostbyname(hostname)
    config = u"""[ req ]
req_extensions     = req_ext
distinguished_name = req_distinguished_name
prompt             = no

[req_distinguished_name]
commonName=%s

[req_ext]
subjectAltName   = @alt_names

[alt_names]
DNS.1  = %s
DNS.2  = %s
DNS.2  = localhost
""" % (ip, hostname, ip)
    config_path = Path('certs/config').resolve()
    if not config_path.parent.exists():
        config_path.parent.mkdir()
    with config_path.open('w') as f:
        f.write(config)
    subprocess.call(['openssl', 'req', '-new', '-newkey', 'rsa:4096', '-days', '365',
                     '-nodes', '-x509', '-subj', '/C=US/ST=Denial/L=Springfield/O=Dis/CN=localhost',
                     '-extensions', 'req_ext', '-config', str(config_path),
                     '-keyout', 'certs/analyst.key',  '-out', 'certs/analyst.cert'])


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

    @app.route('/processors', methods=['GET'])
    def processors():
        return jsonify(scanner.scan_processors())

    @app.route('/zsdk', methods=['GET'])
    def zsdk():
        """Endpoint that returns the zsdk wheel file."""
        wheel = list(Path('/opt/app-root/src/dist/').glob('zsdk*-py2.py3-none-any.whl'))[0]
        return send_file(str(wheel), as_attachment=True, attachment_filename=wheel.name)
