import pytest

from roles.serializers import RoleSerializer
from roles.utils import get_permissions_for_roles


class TestRoleSerializer():

    @pytest.fixture
    def data(self):
        return [
            {'name': 'Test 1',
             'description': 'First Test',
             'permissions': ['permission1', 'permission2']},
            {'name': 'Test 2',
             'description': 'Second Test',
             'permissions': ['permission3']},
        ]

    def test_basic_serialization(self, data):
        serializer = RoleSerializer(data=data, many=True)
        assert serializer.is_valid()
        result = serializer.data
        assert result[0]['name'] == 'Test 1'
        assert len(result) == 2

    def test_missing_permissions(self, data):
        data[1]['permissions'] = []
        serializer = RoleSerializer(data=data, many=True)
        assert not serializer.is_valid()
        errors = serializer.errors
        assert str(errors[1]['permissions'][0]) == 'Ensure this field has at least 1 elements.'

    def test_get_permissions_for_roles(self):
        roles = ['ML_Tools', 'User_Admin']
        permissions = get_permissions_for_roles(roles)
        expected = ['AssetsRead', 'AssetsImport', 'AssetsDelete', 'ProjectManage',
                    'DataSourceManage', 'DataQueueManage', 'ModelTraining']
        assert set(permissions) == set(expected)
        permissions = get_permissions_for_roles(['User_Admin'])
        assert permissions == ['ProjectManage']
