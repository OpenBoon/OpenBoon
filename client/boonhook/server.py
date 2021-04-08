#!/usr/bin/env python3
import logging

import jwt
from flask import Flask, request
from gevent.pywsgi import WSGIServer

logger = logging.getLogger('boonai-webhook')
logging.basicConfig(level=logging.INFO)

app = Flask(__name__)


def validate_webhook_request(headers, secret_key):
    token = headers.get('X-BoonAI-Signature-256', '')
    return jwt.decode(token, secret_key, algorithms=['HS256'])


@app.route('/', methods=['POST'])
def webhook():
    attrs = validate_webhook_request(request.headers, 'bingo')
    logger.info("attrs: {}".fotmat(attrs))
    data = request.data
    logger.info("data: {}".format(data))
    return '', 204


if __name__ == '__main__':
    logger.info('Listening on port 9191')
    server = WSGIServer(('0.0.0.0', 9191), app, log=None)
    server.serve_forever()
