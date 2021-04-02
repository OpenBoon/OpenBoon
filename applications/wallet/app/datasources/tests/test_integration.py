from copy import copy
from uuid import uuid4

import pytest
from django.urls import reverse
from boonsdk import BoonClient, Job
from boonsdk.app import DataSourceApp
from boonsdk.client import BoonSdkDuplicateException
from boonsdk import DataSource

pytestmark = pytest.mark.django_db


@pytest.fixture
def gcp_credentials():
    return {'type': 'GCP',
            'service_account_json_key': r'{"type": "service_account","project_id": "zorroa-rnd","private_key_id": "2abb67ab2ce060c2720","private_key": "-----BEGIN PRIVATE KEY-----\n3Ivqovn4vm1KIUvorP32fi0Em+EDmZqOLx5KG7g5/Z4MgtGUAqZm2aT7yMUC4YAL\nL8s3ZWsQ6aqHjhOUF+2zw5F689Q+n20o5YRWYm1Qy5cN7M+hCbzRqXQpx/fF64nT\npGaDUMQPMvC9yYSHhdmNDO4TkF9LkcS8eDS13/CPBS6sEw8IV8IwUvDkKgMnZSHM\nK6Z6K4iJTNic7qCVCR5IFag3adPVAe9NW7gRkFVZmUKqVBahGb0Gvw75IWCFedfI\nncQGzAVEd9YgYC8JMi8kF3Q3MrMASSzGOINIRcYajEMqgThabnn1RN5lOl2XBWqD\nvu9PdM79AgMBAAECggEAGW2J1LvOwOhljWi1HQpTy2I3iKEDzzEMlnfOpnWma8uB\njAhyHp6Zs3v75G33R19Ra8yAV6xPoQrx5DeYoMVfr+aXoIY6YHFhrqiqz7K0YQ18\nwWvASrla9nsZGUeK3ak9tYTQrnh4Vp6VHq25UV9JkdyGqhvrpdr8JJm8MYnsrqRe\nbaZNM5crVOif1PDadSvv7aTjjRuKXs18giDh7qP6ucsDgVZ02x/zQqSeyvMHos2X\nIO92OEdB2q0n36JVKEY8EFXbRwwsBaQCPPgtJNyjqx4HMYra41fjQGD8gRHCd9B5\n4P/44jzo2IPA4PTDnxOSuV7ynVwCuxZLRjQbvEweAQKBgQDam6aLw6I0tavngdMM\ncnqpFzeA7zBk6gWgKTOreIjLfADLTjvjC7mOlXTQwOmGfg2O//QhOIWd18LzGdtB\neTMKcNAp4sSNedB1VgLItbxoZcB9QAT20ApLiHZUyxHy/wY77EUXjOXSQOvqTuZe\nEjE7EzuXDfTqJQCF9lcmCD/8fQKBgQDIbAvEm5MhaTDFCKGsI27Nfq0IyhCjCsQJ\nanVB+sakm1tRyZXkv3nu23ZKZOq27jrQFHEaSSLZUdre/WRiJBrZ2aLD1MelEROv\nI/Tr/NrpJd5vNYoWA0peK/aZcIm3u/wlXoQvSEa15XEXS2fVAHVbW6tQOCbsmAkx\n8PzIFZkkgQKBgQCKROD2jerOxAnTCD5hJReIU/L7Tk6hxZOBVg4kary7V9d4wCcI\n2KWpFccpMpuCQcB5rlLPoFyDFbFs6fHQW8R42hoQJCqGAYJkdN6V7L0amyFBF3kM\nU1HvrISL5VWZCMz8odihqLDEZ+PP88+puIADCYsrY9yBLJ5EHSfKGnW+UQKBgGqM\njkEKmFCF1KibKyARgkF7G9B1ZBzZh1ieIRJfmKU/9m9npOmEJfWm9J8eQW3Y/qlK\nhMp9oSo5iwtLWMeX/DJeslo7z5tglb9hdT7UISkbucITi4KiYzHnW2U9X+mu5aCU\nO9/LI9Rl0xaYPu4NHVbhSBUQlRjoxtKnFCvm15CBAoGBAMT91VolLmQkZ+CP77eY\nENUDnwA4F7yvTcUHuVeeZfJB0qvMQJOm4meutZoK+ZFZBK71jSvh3SdTgBOmZ7wQ\np6Rk9hg7oXQ2XrR75/Tx1lQXme1Mj+5mKAVk+je/YYlfXmFDy7L0x7FhIKvfG1fF\n7JWeqzsRVoqnKB7GfBKbtzWn\n-----END PRIVATE KEY-----\n","client_email": "187634815620-compute@developer.gserviceaccount.com","client_id": "114806297107550223607","auth_uri": "https://accounts.google.com/o/oauth2/auth","token_uri": "https://accounts.google.com/o/oauth2/token","auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/187634815620-compute%40developer.gserviceaccount.com"}'}  # noqa


@pytest.fixture
def aws_credentials():
    return {'type': 'AWS',
            'aws_access_key_id': 'slkjdflksjflds',
            'aws_secret_access_key': 'kjshdflukenwrfwe'}


@pytest.fixture
def azure_credentials():
    return {'type': 'AZURE',
            'connection_string': 'DefaultEndpointsProtocol=https;AccountName=myAccount;AccountKey=myKey;'}  # noqa


def mock_import_files(*args, **kwargs):
    return Job({'id': '1'})


def mock_zmlp_create_datasource_post_response(*args, **kwargs):
    if args[1] == '/api/v1/credentials':
        return {'id': 'c3ed494f-521c-1224-9431-0603ac5ae8e1',
                'projectId': '00000000-0000-0000-0000-000000000000',
                'name': 'test1', 'type': args[2]['type'], 'timeCreated': 1585948881230,
                'timeModified': 1585948881230,
                'actorCreated': 'f7dba5ce-7eda-3d05-5eb0-9789358c094e/admin-key',
                'actorModified': 'f7dba5ce-7eda-3d05-5eb0-9789358c094e/admin-key'}
    elif args[1] == '/api/v1/data-sources/':
        datasource = {'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'cats', 'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats', 'fileTypes': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'], 'credentials': [], 'modules': [], 'timeCreated': 1583450294925, 'timeModified': 1583450294925, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'}  # noqa)
        if kwargs['body']['credentials']:
            datasource['credentials'] = ["2766f591-5c6a-1ab1-b6d5-0242ac12000b"]
        return datasource


def test_datasource_viewset_create(api_client, monkeypatch, project, zmlp_project_user, login):
    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': {},
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    monkeypatch.setattr(BoonClient, 'post', mock_zmlp_create_datasource_post_response)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 201
    assert response.json()['credentials'] == []
    assert response.json()['name'] == 'cats'
    assert response.json()['jobId'] == '1'


def test_datasource_viewset_create_gcp_creds(api_client, monkeypatch, project,
                                             zmlp_project_user, login, gcp_credentials):
    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': gcp_credentials,
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    monkeypatch.setattr(BoonClient, 'post', mock_zmlp_create_datasource_post_response)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 201
    assert response.json()['name'] == 'cats'


def test_datasource_viewset_create_aws_creds(api_client, monkeypatch, project,
                                             zmlp_project_user, login, aws_credentials):
    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': aws_credentials,
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    monkeypatch.setattr(BoonClient, 'post', mock_zmlp_create_datasource_post_response)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 201
    assert response.json()['name'] == 'cats'


def test_datasource_viewset_create_azure_creds(api_client, monkeypatch, project,
                                               zmlp_project_user, login,
                                               azure_credentials):
    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': azure_credentials,
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    monkeypatch.setattr(BoonClient, 'post', mock_zmlp_create_datasource_post_response)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 201
    assert response.json()['name'] == 'cats'


def test_datasource_viewset_create_null_credentials(api_client, monkeypatch, project,
                                                    zmlp_project_user, login):
    data = {'name': 'cats',
            'uri': 'gs://zorroa-deploy-testdata/zorroa-cypress-testdata/cats',
            'file_types': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff', 'psd'],
            'modules': [],
            'credentials': {},
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    monkeypatch.setattr(BoonClient, 'post', mock_zmlp_create_datasource_post_response)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    assert response.status_code == 201
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
            'credentials': {},
            'id': '96fd6483-5f37-11ea-bb46-6a6895b1a9f6'}

    def mock_post_response(*args, **kwargs):
        raise BoonSdkDuplicateException(data={})

    monkeypatch.setattr(BoonClient, 'post', mock_post_response)
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
        return {'id': '959d7b33-c92f-124a-98af-223772aac895', 'name': 'zmlp-labels', 'description': 'Generate keyword labels for image, video, and documents.', 'restricted': False, 'ops': [{'type': 'APPEND', 'apply': [{'args': {}, 'image': 'zmlp/plugins-analysis', 'module': 'standard', 'className': 'boonai_analysis.mxnet.processors.ResNetClassifyProcessor'}], 'maxApplyCount': 1}], 'timeCreated': 1581354514397, 'timeModified': 1581375180564, 'actorCreated': '00000000-1234-1234-1234-000000000000/background-thread', 'actorModified': '00000000-1234-1234-1234-000000000000/background-thread'}  # noqa

    monkeypatch.setattr(BoonClient, 'post', mock_post_response)
    monkeypatch.setattr(BoonClient, 'get', mock_get_response)
    response = api_client.get(reverse('datasource-list', kwargs={'project_pk': project.id}))
    assert response.json() == {'count': 1, 'next': None, 'previous': None, 'results': [{'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['zmlp-labels'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key', 'url': 'http://testserver/api/v1/projects/6abc33f0-4acf-4196-95ff-4cbb7f640a06/data_sources/48b45e81-4c31-11ea-a1f6-eeae6cabf22b/'}]}  # noqa


def test_datasource_viewset_retrieve(api_client, monkeypatch, zmlp_project_user, project, login):

    def mock_get_response(*args, **kwargs):
        return {'id': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': 'Dev Data Jpgs', 'uri': 'gs://zorroa-dev-data', 'fileTypes': ['jpg', 'jpeg'], 'credentials': [], 'modules': ['959d7b33-c92f-124a-98af-223772aac895'], 'timeCreated': 1581358514511, 'timeModified': 1581358514511, 'actorCreated': 'admin-key', 'actorModified': 'admin-key'}  # noqa

    monkeypatch.setattr(BoonClient, 'get', mock_get_response)
    response = api_client.get(reverse('datasource-detail',
                                      kwargs={'project_pk': project.id,
                                              'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}))
    assert response.status_code == 200
    assert response.json()['name'] == 'Dev Data Jpgs'


def test_datasource_viewset_delete(api_client, monkeypatch, zmlp_project_user, project, login):

    def mock_delete_response(*args, **kwargs):
        return {'success': True}

    monkeypatch.setattr(BoonClient, 'delete', mock_delete_response)
    response = api_client.delete(reverse('datasource-detail',
                                         kwargs={'project_pk': project.id,
                                                 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}))
    assert response.status_code == 200


def test_datasource_viewset_update(api_client, monkeypatch, zmlp_project_user, project, login):
    data = {'id': 'ea7836f4-44a7-19e1-a67f-1e6fa1f82545', 'projectId': '00000000-0000-0000-0000-000000000000', 'name': '2 assets', 'uri': 'gs://zmlp-private-test-data/zorroa-deploy-testdata/zorroa-cypress-testdata/pluto/', 'fileTypes': ['gif', 'png', 'jpg', 'jpeg', 'tif', 'tiff'], 'credentials': [], 'modules': [], 'timeCreated': 1583452262432, 'timeModified': 1585674692983, 'actorCreated': 'admin-key', 'actorModified': 'f7dba5ce-7eda-3d05-5eb0-9789358c094e/admin-key'}  # noqa

    def mock_put_response(*args, **kwargs):
        return data

    def mock_get_datasource(*args, **kwargs):
        return DataSource(data)

    monkeypatch.setattr(BoonClient, 'put', mock_put_response)
    monkeypatch.setattr(DataSourceApp, 'get_datasource', mock_get_datasource)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    kwargs = {'project_pk': project.id, 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}
    response = api_client.put(reverse('datasource-detail', kwargs=kwargs), data)
    assert response.status_code == 200
    assert response.json() == data
    assert response.json()['jobId'] == '1'


def test_datasource_viewset_update_bad_data(api_client, monkeypatch, zmlp_project_user,
                                            project, login):
    data = {'fail': True}
    kwargs = {'project_pk': project.id, 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}
    response = api_client.put(reverse('datasource-detail', kwargs=kwargs), data)
    assert response.status_code == 400
    assert response.json() == {'name': ['This field is required.'],
                               'uri': ['This field is required.']}


def test_datasource_actions_create_scan(api_client, monkeypatch, zmlp_project_user, login, project):
    job_response = {'id': '3875a3e4-4d32-1617-ac79-5a9c7735885a', 'projectId': '8aff54cf-2546-4f67-8861-5e23bb4241b5', 'dataSourceId': '84d73472-e0cf-1bd4-a18a-5a9c7735885a', 'name': 'Applying modules: gcp-video-logo-detection,gcp-video-object-detection,gcp-video-text-detection,gcp-video-label-detection to gs://zorroa-dev-data/pexels', 'state': 'InProgress', 'assetCounts': {'assetTotalCount': 0, 'assetCreatedCount': 0, 'assetReplacedCount': 0, 'assetWarningCount': 0, 'assetErrorCount': 0}, 'taskCounts': {'tasksTotal': 1, 'tasksWaiting': 1, 'tasksRunning': 0, 'tasksSuccess': 0, 'tasksFailure': 0, 'tasksSkipped': 0, 'tasksQueued': 0, 'tasksDepend': 0}, 'timeStarted': -1, 'timeUpdated': 1598308295227, 'timeCreated': 1598308295227, 'timeStopped': -1, 'priority': 100, 'paused': False, 'timePauseExpired': -1, 'maxRunningTasks': 1024, 'jobId': '3875a3e4-4d32-1617-ac79-5a9c7735885a'}  # noqa

    def mock_post_response(*args, **kwargs):
        return job_response

    monkeypatch.setattr(BoonClient, 'post', mock_post_response)
    kwargs = {'project_pk': project.id, 'pk': '48b45e81-4c31-11ea-a1f6-eeae6cabf22b'}
    response = api_client.post(reverse('datasource-scan', kwargs=kwargs), {})
    assert response.json() == {'jobId': job_response['id']}
