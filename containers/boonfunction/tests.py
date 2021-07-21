import logging

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
    print(result)
