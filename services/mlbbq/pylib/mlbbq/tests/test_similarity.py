# flake8: noqa
import io

from boonai_analysis.utils.simengine import SimilarityEngine
from boonflow.testing import test_path

SimilarityEngine.default_model_path = test_path("models/resnet-152")

import unittest.mock as mock

import pytest
import mlbbq.main as server

@pytest.fixture
def client():
    with server.app.test_client() as client:
        yield client


@mock.patch("mlbbq.similarity.check_read_access")
def test_simhash(auth_patch, client):
    auth_patch.return_value = {"name": "test"}
    data = {}
    with open(test_path("images/set01/faces.jpg"), "rb") as fp:
        ufile = io.BytesIO(fp.read())
        data['files'] = [(ufile, 'faces.jpg')]

    response = client.post("/ml/v1/sim-hash", data=data, content_type='multipart/form-data')
    assert 200 == response.status_code
    data = response.get_json()
    assert len(data[0]) == 2048
