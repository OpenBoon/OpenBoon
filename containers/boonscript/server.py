#!/usr/bin/env python3
import logging

from flask import Flask
from gevent.pywsgi import WSGIServer

import endpoint


logger = logging.getLogger('boonai')
logging.basicConfig(level=logging.INFO)

app = Flask(__name__)
endpoint.setup(app)

if __name__ == '__main__':
    logger.info('Listening on port 8080')
    server = WSGIServer(('0.0.0.0', 8080), app, log=None)
    server.serve_forever()
