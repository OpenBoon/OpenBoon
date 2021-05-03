import logging

import flask
from flask import jsonify

from boonai_analysis.utils.simengine import SimilarityEngine
from .auth import check_read_access

logger = logging.getLogger('mlbbq-similarity')

simengine = SimilarityEngine()


def setup_endpoints(app):
    @app.route('/ml/v1/sim-hash', methods=['POST'])
    def get_similarity_hashes():
        user = check_read_access()
        files = flask.request.files.getlist("files")
        logger.info(f"Calculating {len(files)} simhash(s) for {user['name']}")
        try:
            return jsonify([simengine.calculate_simhash(imgdata.stream) for imgdata in files])
        except Exception as e:
            logger.exception("Failed to calculate similarity hash {}".format(e))

        flask.abort(500, description='Unexpected server side exception')
