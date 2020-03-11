from copy import copy
from uuid import uuid4

import pytest
from django.urls import reverse
from zmlp import ZmlpClient
from zmlp.app import DataSourceApp
from zmlp.datasource import DataSource

pytestmark = pytest.mark.django_db


def test_datasource_viewset_create(api_client, monkeypatch, project, zmlp_project_user):
    api_client.force_login(zmlp_project_user)

    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    def mock_create_datasource(*args, **kwargs):
        return DataSource({'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'cats', 'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats', 'fileTypes': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'], 'credentials': [], 'modules': [], 'timeCreated': 1583450294925, 'timeModified': 1583450294925, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'})  # noqa)

    def mock_import_files(*args, **kwargs):
        return None

    monkeypatch.setattr(DataSourceApp, 'create_datasource', mock_create_datasource)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 200
    assert response.json()['credentials'] == []
    assert response.json()['name'] == 'cats'


def test_datasource_viewset_create_null_credentials(api_client, monkeypatch, project,
                                                    zmlp_project_user):
    api_client.force_login(zmlp_project_user)

    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': [],
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    def mock_create_datasource(*args, **kwargs):
        return DataSource({'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'cats', 'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats', 'fileTypes': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'], 'credentials': [], 'modules': [], 'timeCreated': 1583450294925, 'timeModified': 1583450294925, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'})  # noqa)

    def mock_import_files(*args, **kwargs):
        return None

    monkeypatch.setattr(DataSourceApp, 'create_datasource', mock_create_datasource)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 200
    response_data = copy(data)
    response_data['fileTypes'] = response_data['file_types']
    del response_data['file_types']
    response_data['credentials'] = []


def test_datasource_viewset_create_bad_request(api_client, monkeypatch, project, zmlp_project_user):
    api_client.force_login(zmlp_project_user)
    data = {'name': 'test',
            'file_types': ['jpg', 'png'],
            'modules': ['zmlp-labels'],
            'id': str(uuid4())}
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}), data)
    assert response.status_code == 400


def test_datasource_viewset_list(api_client, monkeypatch, zmlp_project_user, project):

    def mock_post_response(*args, **kwargs):
        return {'list': [{'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['959d7b33-c92f-124a-98af-223772aac895'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'}], 'page': {'from': 0, 'size': 50, 'totalCount': 1}}  # noqa

    def mock_get_response(*args, **kwargs):
        return {'id': '959d7b33-c92f-124a-98af-223772aac895', 'name': 'zmlp-labels', 'description': 'Generate keyword labels for image, video, and documents.', 'restricted': False, 'ops': [{'type': 'APPEND', 'apply': [{'args': {}, 'image': 'zmlp/plugins-analysis', 'module': 'standard', 'className': 'zmlp_analysis.mxnet.processors.ResNetClassifyProcessor'}], 'maxApplyCount': 1}], 'timeCreated': 1581354514397, 'timeModified': 1581375180564, 'actorCreated': '00000000-1234-1234-1234-000000000000/background-thread', 'actorModified': '00000000-1234-1234-1234-000000000000/background-thread'}  # noqa

    monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
    monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
    api_client.force_login(zmlp_project_user)
    api_client.force_authenticate(zmlp_project_user)
    response = api_client.get(reverse('datasource-list', kwargs={'project_pk': project.id}))
    assert response.json() == {'count': 1, 'next': None, 'previous': None, 'results': [{'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['zmlp-labels'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key', 'url': 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/datasources/48b45e81-4c31-11ea-a1f6-eeae6cabf22b/'}]}  # noqa


def test_datasource_viewset_retrieve(api_client, monkeypatch, zmlp_project_user, project):

    def mock_get_response(*args, **kwargs):
        return {'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['959d7b33-c92f-124a-98af-223772aac895'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'}  # noqa

    monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
    api_client.force_login(zmlp_project_user)
    api_client.force_authenticate(zmlp_project_user)
    response = api_client.get(reverse('datasource-detail',
                                      kwargs={'project_pk': project.id,
                                              'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}))
    assert response.status_code == 200
    assert response.json()['name'] == 'Dev Data Jpgs'


def test_datasource_viewset_delete(api_client, monkeypatch, zmlp_project_user, project):

    def mock_delete_response(*args, **kwargs):
        return {'detail': 'success'}  # noqa

    monkeypatch.setattr(ZmlpClient, 'delete', mock_delete_response)
    api_client.force_login(zmlp_project_user)
    api_client.force_authenticate(zmlp_project_user)
    response = api_client.delete(reverse('datasource-detail',
                                         kwargs={'project_pk': project.id,
                                                 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}))
    assert response.status_code == 200
