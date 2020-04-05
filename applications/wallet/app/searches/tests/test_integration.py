import pytest
from django.urls import reverse
from rest_framework import status

from searches.models import Search
from wallet.tests.utils import check_response

pytestmark = pytest.mark.django_db


@pytest.fixture()
def query():
    return {
        'query': {
            "prefix": {
                "files.name": {
                    "value": "image"
                }
            }
        }
    }


@pytest.fixture()
def search(project, user, query):
    return Search.objects.create(project=project, name='Test Search', search=query,
                                 created_by=user)


class TestSearchViewSetList:

    def test_empty_list(self, zmlp_project_membership, login, api_client, project):
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        content = check_response(response)
        assert content['results'] == []

    def test_list(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'Test Search'
        assert results[0]['project'].endswith(f'{project.id}/')
        assert results[0]['search']['query']['prefix']['files.name']['value'] == 'image'
        assert results[0]['createdBy'].endswith(f'{zmlp_project_membership.user.id}/')

    def test_list_filters_by_project(self, zmlp_project_membership, login, api_client, project,
                                     project2, search, query):
        Search.objects.create(project=project2, name='Other Project Search', search=query,
                              created_by=zmlp_project_membership.user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'Test Search'
        assert results[0]['project'].endswith(f'{project.id}/')


class TestSearchViewSetRetrieve:

    def test_retrieve(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.get(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': search.id}))
        result = check_response(response)
        assert result['name'] == 'Test Search'
        assert result['project'].endswith(f'{project.id}/')
        assert result['search']['query']['prefix']['files.name']['value'] == 'image'
        assert result['createdBy'].endswith(f'{zmlp_project_membership.user.id}/')


class TestSearchViewSetCreate:

    def assert_created(self, content, project, query, user):
        """Helper to check the standard create worked for this test set."""
        assert content['project'].endswith(f'{project.id}/')
        assert content['name'] == 'Tester'
        assert content['search'] == query
        assert content['createdDate']
        assert content['modifiedDate']
        assert content['createdBy'].endswith(f'{user.id}/')

    def test_create(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": query,
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}), body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)

    def test_no_project(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "name": "Tester",
            "search": query,
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)

    def test_no_name(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "search": query,
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        assert content['name'] == ['This field is required.']

    def test_no_search(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "created_by": str(user.id)
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        assert content['search'] == ['This field is required.']

    def test_no_created_by(self, zmlp_project_membership, login, api_client, query, project, user):
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": query,
        }
        response = api_client.post(reverse('search-list', kwargs={'project_pk': str(project.id)}),
                                   body)  # noqa
        content = check_response(response, status.HTTP_201_CREATED)
        self.assert_created(content, project, query, user)


class TestSearchViewSetUpdate:

    def test_update(self, zmlp_project_membership, login, api_client, query, project, user):
        search = Search.objects.create(project=project, name='My Old Search',
                                       search=query, created_by=user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'My Old Search'

        new_query = {"hey": "you guys"}
        body = {
            "project": str(project.id),
            "name": "Tester",
            "search": new_query,
            "created_by": str(user.id)
        }
        response = api_client.put(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': str(search.id)}), body)
        content = check_response(response, status.HTTP_200_OK)
        assert content['id'] == str(search.id)
        assert content['name'] == 'Tester'
        assert content['search'] == new_query

    def test_partial_with_bad_method(self, zmlp_project_membership, login, api_client, query,
                                     project, user):
        search = Search.objects.create(project=project, name='My Old Search',
                                       search=query, created_by=user)

        new_query = {"hey": "you guys"}
        body = {
            "search": new_query,
        }
        response = api_client.put(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                   'pk': str(search.id)}), body)
        content = check_response(response, status.HTTP_400_BAD_REQUEST)
        for field in ('project', 'name', 'createdBy'):
            assert content[field] == ['This field is required.']

    def test_partial_update(self, zmlp_project_membership, login, api_client, query, project, user):
        search = Search.objects.create(project=project, name='My Old Search',
                                       search=query, created_by=user)
        response = api_client.get(reverse('search-list', kwargs={'project_pk': str(project.id)}))
        results = check_response(response)['results']
        assert len(results) == 1
        assert results[0]['name'] == 'My Old Search'

        new_query = {"hey": "you guys"}
        body = {
            "search": new_query,
        }
        response = api_client.patch(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                     'pk': str(search.id)}), body)
        content = check_response(response, status.HTTP_200_OK)
        assert content['id'] == str(search.id)
        assert content['name'] == 'My Old Search'
        assert content['search'] == new_query


class TestSearchDestroy:

    def test_destroy(self, zmlp_project_membership, login, api_client, project, search):
        response = api_client.delete(reverse('search-detail', kwargs={'project_pk': str(project.id),
                                                                      'pk': str(search.id)}))
        assert response.status_code == status.HTTP_204_NO_CONTENT

    def test_destroy_non_member(self, zmlp_project_membership, login, api_client, project2, query):
        search2 = Search.objects.create(project=project2, name='Other Project Search', search=query,
                                        created_by=zmlp_project_membership.user)
        response = api_client.delete(reverse('search-detail', kwargs={'project_pk': str(project2.id),  # noqa
                                                                      'pk': str(search2.id)}))
        assert response.status_code == status.HTTP_403_FORBIDDEN
