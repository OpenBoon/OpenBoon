import pytest
from unittest.mock import patch, Mock

from wallet.utils import (sync_project_with_zmlp, sync_membership_with_zmlp,
                          convert_json_to_base64, convert_base64_to_json,
                          validate_zmlp_data)
from boonsdk import BoonClient
from boonsdk.client import BoonSdkNotFoundException
from projects.models import Project
from wallet.exceptions import InvalidZmlpDataError


pytestmark = pytest.mark.django_db


class TestValidateZmlpData:

    def test_raises(self):
        serializer = Mock(is_valid=Mock(return_value=False))
        with pytest.raises(InvalidZmlpDataError):
            validate_zmlp_data(serializer)


class TestConversions:

    def test_convert_json(self):
        data = {'test': 'testing'}
        converted = convert_json_to_base64(data)
        assert convert_base64_to_json(converted) == data

    def test_convert_base64(self):
        data = b'eyJ0ZXN0IjogInRlc3RpbmcifQ=='
        converted = convert_base64_to_json(data)
        assert converted == {'test': 'testing'}


class TestSyncProject:

    @patch.object(BoonClient, 'get')
    def test_sync_when_synced(self, get_mock, project):
        get_mock.return_value = {'id': project.id,
                                 'name': project.name,
                                 'enabled': project.isActive}
        project.apikey = {'id': 'test'}
        sync_project_with_zmlp(project)
        get_mock.assert_called_once()

    @patch.object(BoonClient, 'put')
    @patch.object(BoonClient, 'get')
    @patch.object(BoonClient, 'post')
    @patch('apikeys.utils.create_zmlp_api_key')
    def test_sync_when_not_synced(self, create_key_mock, post_mock, get_mock, put_mock, project):
        get_mock.side_effect = BoonSdkNotFoundException(data={'message': 'no good'})
        post_mock.return_value = {'id': 'test',
                                  'name': 'No Name',
                                  'enabled': not project.isActive}
        put_mock.side_effect = ({}, {'success': True})
        create_key_mock.return_value = 'asdf'

        sync_project_with_zmlp(project, create=True)

        project = Project.objects.get(id=project.id)
        assert project.apikey == 'asdf'

    @patch.object(BoonClient, 'put')
    @patch.object(BoonClient, 'get')
    @patch.object(BoonClient, 'post')
    @patch('apikeys.utils.create_zmlp_api_key')
    def test_sync_when_not_active_and_not_successful(self, create_key_mock, post_mock,
                                                     get_mock, put_mock, project):
        project.isActive = False
        project.save()
        get_mock.side_effect = BoonSdkNotFoundException(data={'message': 'no good'})
        post_mock.return_value = {'id': 'test',
                                  'name': 'No Name',
                                  'enabled': not project.isActive}
        put_mock.side_effect = ({}, {'success': False})
        create_key_mock.return_value = 'asdf'

        with pytest.raises(IOError):
            sync_project_with_zmlp(project, create=True)


class TestSyncMembership:

    @patch.object(BoonClient, 'get')
    @patch('apikeys.utils.create_zmlp_api_key')
    def test_sync_admin_key(self, create_key_mock, get_mock, zmlp_project_membership):
        create_key_mock.return_value = 'asdf'
        get_mock.return_value = {'permissions': []}

        with pytest.raises(ValueError):
            sync_membership_with_zmlp(zmlp_project_membership)
