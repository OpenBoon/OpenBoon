from uuid import uuid4

import pytest
from django.urls import reverse
from zmlp.app import DataSourceApp
from zmlp.datasource import DataSource

pytestmark = pytest.mark.django_db


def test_datasource_list_create(api_client, monkeypatch, project, zmlp_project_user):
    api_client.force_login(zmlp_project_user)
    data = {'name': 'test',
            'uri': 'gs://test-bucket',
            'file_types': ['jpg', 'png'],
            'modules': ['zmlp-labels']}

    def mock_create_datasource(*args, **kwargs):
        return DataSource(data)

    def mock_import_files(*args, **kwargs):
        return None

    monkeypatch.setattr(DataSourceApp, 'create_datasource', mock_create_datasource)
    monkeypatch.setattr(DataSourceApp, 'import_files', mock_import_files)
    response = api_client.post(reverse('datasource-list', kwargs={'project_pk': project.id}),
                               data)
    data['id'] = uuid4()
    assert response == data
