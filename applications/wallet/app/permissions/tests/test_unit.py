import pytest
from django.urls import reverse

from permissions.serializers import PermissionSerializer


@pytest.fixture
def data():
    return [
        {'name': 'SystemMonitor',
         'description': 'Allows access to monitoring endpoints'},
        {'name': 'SystemManage',
         'description': 'Allows access to platform management endpoints'},
        {'name': 'SystemProjectOverride',
         'description': 'Provides ability to switch projects'},
        {'name': 'SystemProjectDecrypt',
         'description': 'Provides ability to view encrypted project data'}
    ]


@pytest.fixture
def context(api_factory):
    request = api_factory.get(reverse('permission-list', kwargs={'project_pk': 'id'}))
    return {'request': request}


def test_permissions_all_field_validation(data, context):
    serializer = PermissionSerializer(data=data, context=context, many=True)
    assert serializer.is_valid()
    validated = serializer.validated_data
    assert validated[0]['name'] == data[0]['name']
    assert validated[0]['description'] == data[0]['description']


def test_bad_validation(data, context):
    data[0]['description'] = True
    serializer = PermissionSerializer(data=data, context=context, many=True)
    assert not serializer.is_valid()
    error = serializer.errors[0]
    assert error['description'][0].title() == 'Not A Valid String.'
