# flake8: noqa
import json
import logging
import unittest.mock as mock

import pytest

import mlbbq.main as server
from boonflow.testing import test_path
from boonflow.base import ImageInputStream
from boonsdk import BoonClient
from mlbbq.modules import setup_endpoints

from boonai_analysis.boonai import ZviFaceDetectionProcessor

logging.basicConfig(level=logging.INFO)
setup_endpoints(server.app)


@pytest.fixture
def client():
    with server.app.test_client() as client:
        yield client


@mock.patch.object(BoonClient, 'post')
@mock.patch("mlbbq.modules.check_write_access")
@mock.patch.object(ZviFaceDetectionProcessor, 'load_proxy_image')
def test_apply_to_asset(proxy_patch, auth_patch, post_patch, client):
    image_path = test_path('images/face-recognition/face1.jpg')
    proxy_patch.return_value = ImageInputStream.from_path(image_path)

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
                "id": "TRANSIENT"
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


@mock.patch("mlbbq.modules.check_write_access")
def test_apply_no_modules(auth_patch, client):
    image_path = test_path('images/face-recognition/face1.jpg')

    with open(image_path, 'rb') as fp:
        rsp = client.post("/ml/v1/modules/apply-to-file",
                          data=fp, content_type='application/octet-stream')
    assert rsp.status_code == 400


@mock.patch.object(BoonClient, 'post')
@mock.patch("mlbbq.modules.check_write_access")
def test_apply_to_file_bad_data(auth_patch, post_patch, client):
    # Unsupported image type
    image_path = test_path('images/set06/dpx_nuke_16bits_rgba.dpx')

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
                "id": "TRANSIENT"
            }
        ]
    }
    post_patch.return_value = script
    with open(image_path, 'rb') as fp:
        rsp = client.post("/ml/v1/modules/apply-to-file?modules=boonai-face-detection",
                          data=fp, content_type='application/octet-stream')
    assert rsp.status_code == 412
