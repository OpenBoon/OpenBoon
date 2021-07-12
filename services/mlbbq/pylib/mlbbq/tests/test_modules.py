# flake8: noqa
import json
import logging
import unittest.mock as mock

import pytest

import mlbbq.main as server
from boonflow.testing import test_path
from boonsdk import BoonClient
from mlbbq.modules import setup_endpoints

logging.basicConfig(level=logging.INFO)


@pytest.fixture
def client():
    setup_endpoints(server.app)
    with server.app.test_client() as client:
        yield client


@mock.patch.object(BoonClient, 'post')
@mock.patch("mlbbq.modules.check_write_access")
@mock.patch('boonai_analysis.boonai.faces.get_proxy_level_path')
def test_apply_to_asset(proxy_patch, auth_patch, post_patch, client):
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

    rsp = client.post("/ml/v1/modules/apply-to-asset", data=json.dumps(req), content_type='application/json')
    assert rsp.status_code == 200

    result = rsp.get_json()
    assert result['document']['analysis']['boonai-face-detection']['count'] == 1


@mock.patch.object(BoonClient, 'post')
@mock.patch("mlbbq.modules.check_write_access")
def test_apply_to_file(auth_patch, post_patch, client):
    image_path = test_path('images/face-recognition/face1.jpg')

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
            }
        ]
    }
    post_patch.return_value = script
    with open(image_path, 'rb') as fp:
        rsp = client.post("/ml/v1/modules/apply-to-file?modules=boonai-face-detection",
                          data=fp, content_type='application/octet-stream')
    assert rsp.status_code == 200

    result = rsp.get_json()
    assert result['document']['analysis']['boonai-face-detection']['count'] == 1
