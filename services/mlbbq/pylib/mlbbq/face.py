import logging
import flask
import io
from flask import jsonify

from .auth import check_read_access
from boonsdk.util import to_json
from boonai_analysis.boonai.faces import MtCnnFaceDetectionEngine

logger = logging.getLogger('mlbbq-faces')


engine = MtCnnFaceDetectionEngine()


def setup_endpoints(app):

    @app.route('/ml/v1/face-detection', methods=['POST'])
    def detect_faces_upload():
        user = check_read_access()
        image = io.BytesIO(flask.request.data)

        logger.info(f"Detecting faces for {user['name']}")
        analysis = engine.get_analysis(image)
        return jsonify(to_json(analysis))
