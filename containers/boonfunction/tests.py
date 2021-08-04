import logging
import sys
import json

import pytest

import server
from boonflow.testing import TestAsset
from boonsdk import to_json

logging.basicConfig(level=logging.DEBUG)


@pytest.fixture
def client():
    with server.app.test_client() as client:
        yield client


def test_function(client):
    asset = TestAsset()
    rsp = client.post("/", data=to_json(asset), content_type='application/json')

    assert rsp.status_code == 200

    result = rsp.get_json()
    assert 'analysis' in result
    assert 'custom-fields' in result


def test_custom_error(client):
    rsp = server.custom_error('failure')
    assert rsp.content_type == 'application/json'
    assert rsp.status_code == 511


def test_custom_error(client):

    try:
        raise RuntimeError("test")
    except RuntimeError:
        rsp = server.custom_error('failure', sys.exc_info()[2])
        assert rsp.content_type == 'application/json'
        assert rsp.status_code == 511
        msg = json.loads(rsp.data)

        assert 'errorId' in msg
        assert 'stackTrace' in msg
        assert msg['code'] == 551
        assert msg['message'] == 'failure'
        assert msg['path'] == '/'
        assert msg['exception'] == 'BoonFunctionException'
