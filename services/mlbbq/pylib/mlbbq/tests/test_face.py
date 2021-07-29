# flake8: noqa
import logging
import unittest.mock as mock

import pytest

from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow.testing import test_path

SimilarityEngine.default_model_path = test_path("models/resnet-152")
import mlbbq.main as server

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def client():
    with server.app.test_client() as client:
        yield client


@mock.patch("mlbbq.face.check_read_access")
def test_face(auth_patch, client):
    auth_patch.return_value = {"name": "test"}
    with open(test_path("images/set01/faces.jpg"), "rb") as fp:
        data = fp.read()

    response = client.post("/ml/v1/face-detection", data=data,
                           content_type='application/octet-stream')
    data = response.get_json()
    print(data)
