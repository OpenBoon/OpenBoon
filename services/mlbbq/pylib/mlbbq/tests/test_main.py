# flake8: noqa
import io
import os
import unittest.mock as mock

import pytest

from boonflow.testing import test_path

os.environ['SIMHASH_MODEL_PATH'] = test_path("models/resnet-152")
# Have to set env before this endpoint
import mlbbq.main as server


@pytest.fixture
def client():
    with server.app.test_client() as client:
        yield client


@mock.patch("mlbbq.main.authenticate")
def test_simhash(auth_patch, client):
    auth_patch.return_value = {"name": "test"}
    data = {}
    with open(test_path("images/set01/faces.jpg"), "rb") as fp:
        ufile = io.BytesIO(fp.read())
        data['files'] = [(ufile, 'faces.jpg')]

    response = client.post("/ml/v1/sim-hash", data=data, follow_redirects=True,
                           content_type='multipart/form-data')
    data = response.get_json()
    assert len(data[0]) == 2048
