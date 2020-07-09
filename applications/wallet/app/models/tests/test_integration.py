import pytest
from django.urls import reverse
from rest_framework import status
from zmlp import ZmlpClient

from wallet.tests.utils import check_response

pytestmark = pytest.mark.django_db


class TestModelViewSetList:

    def test_list(self, login, project, api_client):
        path = reverse('model-list', kwargs={'project_pk': project.id})
        response = api_client.get(path)
        check_response(response)


class TestModelViewSetRetrieve:

    def test_retrieve(self, login, project, api_client):
        model_id = 'model_id'
        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        response = api_client.get(path)
        check_response(response)


class TestModelViewSetCreate:

    def test_create(self, login, project, api_client):
        body = {'name': 'Test Model',
                'type': 'model_type'}
        path = reverse('model-list', kwargs={'project_pk': project.id})
        response = api_client.post(path, body)
        check_response(response, status.HTTP_201_CREATED)


class TestModelViewSetDestroy:

    def test_destroy(self, login, project, api_client):
        model_id = 'model_id'
        path = reverse('model-detail', kwargs={'project_pk': project.id,
                                               'pk': model_id})
        response = api_client.delete(path)
        check_response(response)


class TestModelViewSetActions:

    def test_model_types(self, login, project, api_client):
        path = reverse('model-model-types', kwargs={'project_pk': project.id})
        response = api_client.get(path)
        check_response(response)

    def test_train(self, login, project, api_client):
        model_id = 'model_id'
        path = reverse('model-train', kwargs={'project_pk': project.id,
                                              'pk': model_id})
        response = api_client.post(path)
        check_response(response)
