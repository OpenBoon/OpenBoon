# flake8: noqa
import json
import logging
import unittest.mock as mock

import pytest

import mlbbq.main as server
from boonflow.testing import test_path
from boonsdk import BoonClient
from mlbbq.pipeline import setup_endpoints

logging.basicConfig(level=logging.INFO)

@pytest.fixture
def client():
    setup_endpoints(server.app)
    with server.app.test_client() as client:
        yield client


@mock.patch.object(BoonClient, 'post')
@mock.patch("mlbbq.pipeline.check_write_access")
@mock.patch('boonai_analysis.boonai.faces.get_proxy_level_path')
def test_pipeline(proxy_patch, auth_patch, post_patch, client):
    image_path = test_path('images/face-recognition/face1.jpg')
    proxy_patch.return_value = image_path

    script = {
        "execute": [
            {
                "className": "boonai_analysis.boonai.ZviFaceDetectionProcessor",
                "args": {},
                "image": "boonai/plugins-analysis",
                "module": "boonai-face-detection"
            }
        ],
        "assets": [
            {
                "id": "123456",
                "document": {
                    "source": {
                        "path": "gs://zorroa-public/cat.jpg"
                    }
                }
            }
        ]
    }
    post_patch.return_value = script
    req = {
        "assetId": "123456",
        "modules": ["boonai-face-detection"],
        "index": False
    }

    rsp = client.post("/ml/v1/apply-modules", data=json.dumps(req), content_type='application/json')
    assert rsp.status_code == 200

    result = rsp.get_json()
    assert result['document']['analysis']['boonai-face-detection']['count'] == 1
