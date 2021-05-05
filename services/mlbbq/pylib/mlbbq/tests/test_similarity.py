# flake8: noqa
import io
import os
import logging
import unittest.mock as mock

import pytest

from boonflow.testing import test_path
from mlbbq.main import setup_endpoints

os.environ['SIMHASH_MODEL_PATH'] = test_path("models/resnet-152")
# Have to set env before this endpoint
import mlbbq.main as server


logging.basicConfig(level=logging.INFO)


@pytest.fixture
def client():
    setup_endpoints()
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
