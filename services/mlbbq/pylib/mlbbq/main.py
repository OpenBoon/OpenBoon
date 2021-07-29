#!/usr/bin/env python3

import logging
import os
import importlib
import random
import string

from flask import Flask, jsonify, g

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024


@app.before_request
def before_request():
    request_id = ''.join(random.choices(string.ascii_uppercase + string.digits, k=24))
    g.request_id = request_id


def setup_endpoints():
    modules = ["similarity", "face", "modules"]
    for mod in modules:
        logger.info(f"setting up endpoints for {mod}")
        imported = importlib.import_module(f'.{mod}', package="mlbbq")
        imported.setup_endpoints(app)
    logger.info("Done setting up endopoints")


@app.route('/healthcheck', methods=['GET'])
def get_heath_check():
    return jsonify({'healthy': True})


logger = logging.getLogger('mlbbq')
flask_log = logging.getLogger('werkzeug')
flask_log.disabled = True
app.logger.disabled = True

if os.environ.get("BOONAI_DEBUG"):
    logging.basicConfig(level=logging.DEBUG)
else:
    logging.basicConfig(level=logging.INFO)

setup_endpoints()
