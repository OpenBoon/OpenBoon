#!/usr/bin/env python3

import argparse
import logging
import os
import flask
import requests

from flask import Flask, jsonify
from gevent.pywsgi import WSGIServer

from .simhash import get_similarity_hash, SimilarityModel

app = Flask(__name__)
logger = logging.getLogger(__name__)

auth_url = os.environ.get("BOONAI_SECURITY_AUTHSERVER_URL", "http://auth-server:9090")
auth_endpoint = "{}/auth/v1/auth-token".format(auth_url)

SimilarityModel.load()

def main():
    parser = argparse.ArgumentParser(prog='zmld')
    parser.add_argument("-p", "--port", help="The port to listen on",
                        default=os.environ.get("BOONAI_PORT", "8282"))
    parser.add_argument("-v", "--verbose", help="Debg logging",
                        action="store_true")

    args = parser.parse_args()
    if os.environ.get("BOONAI_DEBUG") or args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    flask_log = logging.getLogger('werkzeug')
    flask_log.disabled = True
    app.logger.disabled = True

    print("Listening on port {}".format(args.port))
    server = WSGIServer(('0.0.0.0', int(args.port)), app)
    server.serve_forever()


@app.route('/ml/v1/sim-hash', methods=['POST'])
def get_similarity_hashes():
    authenticate(flask.request.headers.get("Authorization"))
    files = flask.request.files.getlist("files")
    try:
        return jsonify([get_similarity_hash(imgdata.stream) for imgdata in files])
    except Exception as e:
        logger.exception("Failed to calculate similarity hash {}".format(e))


@app.route('/healthcheck', methods=['GET'])
def get_heath_check():
    return jsonify({'healthy': True})


def authenticate(token):
    """
    Authenticate the given token.  Throws if auth fails.

    Args:
        token (str): A JTW token

    """
    rsp = requests.post(auth_endpoint, headers={"Authorization": token})
    if rsp.status_code != 200:
        raise Exception("Access denied")



