from copy import copy
from uuid import uuid4

import pytest
from django.urls import reverse
from zmlp import ZmlpClient
from zmlp.app import DataSourceApp
from zmlp.client import ZmlpDuplicateException
from zmlp.datasource import DataSource

pytestmark = pytest.mark.django_db


def test_datasource_viewset_create(api_client, monkeypatch, project, zmlp_project_user, login):
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
                                                    zmlp_project_user, login):
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


def test_datasource_viewset_create_duplicate(api_client, monkeypatch, project, zmlp_project_user,
                                             login):
    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': [],
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    def mock_create_datasource(*args, **kwargs):
        raise ZmlpDuplicateException(data={})

    monkeypatch.setattr(DataSourceApp, 'create_datasource', mock_create_datasource)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 409
    assert response.json() == {'name': ['A Data Source with that name already exists.']}


def test_datasource_viewset_create_bad_request(api_client, monkeypatch, project, zmlp_project_user):
    api_client.force_login(zmlp_project_user)
    data = {'name': 'test',
            'file_types': ['jpg', 'png'],
            'modules': ['zmlp-labels'],
            'id': str(uuid4())}
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}), data)
    assert response.status_code == 400


def test_datasource_viewset_list(api_client, monkeypatch, zmlp_project_user, project, login):

    def mock_post_response(*args, **kwargs):
        return {'list': [{'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['959d7b33-c92f-124a-98af-223772aac895'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'}], 'page': {'from': 0, 'size': 50, 'totalCount': 1}}  # noqa

    def mock_get_response(*args, **kwargs):
        return {'id': '959d7b33-c92f-124a-98af-223772aac895', 'name': 'zmlp-labels', 'description': 'Generate keyword labels for image, video, and documents.', 'restricted': False, 'ops': [{'type': 'APPEND', 'apply': [{'args': {}, 'image': 'zmlp/plugins-analysis', 'module': 'standard', 'className': 'zmlp_analysis.mxnet.processors.ResNetClassifyProcessor'}], 'maxApplyCount': 1}], 'timeCreated': 1581354514397, 'timeModified': 1581375180564, 'actorCreated': '00000000-1234-1234-1234-000000000000/background-thread', 'actorModified': '00000000-1234-1234-1234-000000000000/background-thread'}  # noqa

    monkeypatch.setattr(ZmlpClient, 'post', mock_post_response)
    monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
    response = api_client.get(reverse('datasource-list', kwargs={'project_pk': project.id}))
    assert response.json() == {'count': 1, 'next': None, 'previous': None, 'results': [{'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['zmlp-labels'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key', 'url': 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/data_sources/48b45e81-4c31-11ea-a1f6-eeae6cabf22b/'}]}  # noqa


def test_datasource_viewset_retrieve(api_client, monkeypatch, zmlp_project_user, project, login):

    def mock_get_response(*args, **kwargs):
        return {'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['959d7b33-c92f-124a-98af-223772aac895'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'}  # noqa

    monkeypatch.setattr(ZmlpClient, 'get', mock_get_response)
    response = api_client.get(reverse('datasource-detail',
                                      kwargs={'project_pk': project.id,
                                              'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}))
    assert response.status_code == 200
    assert response.json()['name'] == 'Dev Data Jpgs'


def test_datasource_viewset_delete(api_client, monkeypatch, zmlp_project_user, project, login):

    def mock_delete_response(*args, **kwargs):
        return {'detail': 'success'}  # noqa

    monkeypatch.setattr(ZmlpClient, 'delete', mock_delete_response)
    response = api_client.delete(reverse('datasource-detail',
                                         kwargs={'project_pk': project.id,
                                                 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}))
    assert response.status_code == 200


def test_datasource_viewset_update(api_client, monkeypatch, zmlp_project_user, project, login):
    data = {'id': 'ea7836f4-44a7-19e1-a67f-1e6fa1f82545', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': '2 assets', 'uri': 'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-cypress-testdata/pluto/', 'fileTypes': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff'], 'credentials': [], 'modules': [], 'timeCreated': 1583452262432, 'timeModified': 1585674692983, 'actorCreated': 'admin-key', 'actorModified': 'f7dba5ce-7eda-3d05-5eb0-9789358c094e/admin-key'}  # noqa

    def mock_put_response(*args, **kwargs):
        return data

    monkeypatch.setattr(ZmlpClient, 'put', mock_put_response)
    kwargs = {'project_pk': project.id, 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}
    response = api_client.put(reverse('datasource-detail', kwargs=kwargs), data)
    assert response.status_code == 200
    assert response.json() == data


def test_datasource_viewset_update_bad_data(api_client, monkeypatch, zmlp_project_user,
                                            project, login):
    data = {'fail': True}
    kwargs = {'project_pk': project.id, 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}
    response = api_client.put(reverse('datasource-detail', kwargs=kwargs), data)
    assert response.status_code == 400
    assert response.json() == {'name': ['This field is required.'],
                               'uri': ['This field is required.']}
