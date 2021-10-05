import pytest
from uuid import UUID
from django.urls import reverse

from apikeys.serializers import ApikeySerializer


@pytest.fixture
def data():
    return {
        'id': 'b3a09695-b9fb-40bd-8ea8-bbe0c2cba33f',
        'name': 'Test',
        'projectId': '2fb4e52b-8791-4544-aafb-c16af66f19f8',
        'accessKey': 'myAccessKey',
        'secretKey': 'mySecretKey',
        'permissions': ['AssetsRead']
    }


@pytest.fixture
def context(api_factory):
    request = api_factory.get(reverse('apikey-list', kwargs={'project_pk': 'id'}))
    return {'request': request}


def test_apikey_all_field_validation(data, context):
    serializer = ApikeySerializer(data=data, context=context)
    assert serializer.is_valid()
    validated = serializer.validated_data
    assert validated['id'] == UUID(data['id'])
    assert validated['name'] == data['name']
    assert validated['projectId'] == UUID(data['projectId'])
    assert validated['accessKey'] == data['accessKey']
    assert validated['secretKey'] == data['secretKey']
    assert validated['permissions'] == data['permissions']


def test_apikey_url_generation(data, context):
    serializer = ApikeySerializer(data=data, context=context)
    assert serializer.is_valid()
    assert serializer.data['url'].endswith(
        f'api/v1/projects/id/api_keys/{serializer.data["id"]}/'
    )


def test_minimum_valid_data():
    data = {'name': 'Test', 'permissions': ['AssetsRead']}
    serializer = ApikeySerializer(data=data)
    assert serializer.is_valid()
    assert serializer.validated_data['name'] == 'Test'
    assert serializer.validated_data['permissions'] == ['AssetsRead']
