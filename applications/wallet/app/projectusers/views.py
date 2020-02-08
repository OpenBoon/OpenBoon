from rest_framework import status
from rest_framework.response import Response
from django.contrib.auth import get_user_model
from django.http import Http404
from django.db import transaction
from zmlp.client import ZmlpInvalidRequestException

from projects.views import BaseProjectViewSet
from projects.models import Project, Membership
from projectusers.serializers import ProjectUserSerializer
from wallet.paginators import FromSizePagination
from apikeys.utils import decode_apikey, encode_apikey

User = get_user_model()


class ProjectUserViewSet(BaseProjectViewSet):
    """Users who are Members of this Project.

    Available HTTP methods, endpoints, and what they do:

    * **GET** _api/v1/projects/$Project_Id/users/_ - List the Users who are members of $Project_Id
    * **GET** _api/v1/projects/$Project_Id/users/$User_Id/_ - Detail info on $User_Id
    * **POST** _api/v1/projects/$Project_Id/users/_ - Create membership/s to $Project_Id
        - To create one: `{"email": "user@email.com", "permissions": ["AssetsRead"]}`
        - To create multiple: `{"batch": [
                                    {"email": "user@email.com", "permissions": ["AssetsRead"]},
                                    {"email": "user2@email.com", "permissions": ["AssetsRead"]}
                                ]}`
    * **PUT/PATCH** _api/v1/projects/$Project_Id/users/$User_Id/_ - Update a Users permissions
    * **DELETE** _api/v1/projects/$Project_Id/users/$User_Id/_ - Remove $User_Id from $Project_Id

    """

    ZMLP_ONLY = True
    pagination_class = FromSizePagination
    serializer_class = ProjectUserSerializer

    def get_object(self, pk, project_pk):
        try:
            return Membership.objects.get(user=pk, project=project_pk)
        except Membership.DoesNotExist:
            raise Http404

    def get_project_object(self, pk):
        try:
            return Project.objects.get(id=pk)
        except Project.DoesNotExist:
            raise Http404

    def list(self, request, project_pk):
        # Need to handle pagination
        # If the project doesn't exist or user is not a member a 403 is returned
        project = self.get_project_object(project_pk)
        users = project.users.all()
        paginated_users = self.paginate_queryset(users)
        if paginated_users is not None:
            serializer = self.get_serializer(paginated_users, many=True)
            return self.get_paginated_response(serializer.data)
        serializer = self.get_serializer(users, many=True)
        return Response(data={'results': serializer.data}, status=status.HTTP_200_OK)

    def retrieve(self, request, project_pk, pk):
        # List details about the current project User
        user = self.get_object(pk, project_pk).user
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    def create(self, request, project_pk):
        batch = request.data.get('batch')
        if batch and request.data.get('email'):
            return Response(data={'detail': 'Batch argument provided with single creation arguments.'},  # noqa
                            status=status.HTTP_400_BAD_REQUEST)
        elif batch:
            response_body = {'results': {'succeeded': [], 'failed': []}}
            for entry in batch:
                response = self._create_project_user(request, project_pk, entry)
                content = {'email': entry.get('email'),
                           'permissions': entry.get('permissions'),
                           'status_code': response.status_code,
                           'body': response.data}
                if response.status_code == status.HTTP_201_CREATED:
                    response_body['results']['succeeded'].append(content)
                else:
                    response_body['results']['failed'].append(content)
            return Response(data=response_body, status=status.HTTP_207_MULTI_STATUS)
        else:
            return self._create_project_user(request, project_pk, request.data)

    def _create_project_user(self, request, project_pk, data):
        # Get the User and add the appropriate Membership & ApiKey
        try:
            email = data['email']
        except KeyError:
            return Response(data={'detail': 'No email given.'},
                            status=status.HTTP_400_BAD_REQUEST)
        try:
            permissions = data['permissions']
        except KeyError:
            return Response(data={'detail': 'No permissions given.'},
                            status=status.HTTP_400_BAD_REQUEST)
        # Get the current project
        project = self.get_project_object(project_pk)
        # Search for an existing user
        try:
            user = User.objects.get(username=email)
        except User.DoesNotExist:
            # Should we actually silently fail and return a 200 here?
            return Response(data={'detail': 'No user with the given email.'},
                            status=status.HTTP_404_NOT_FOUND)
        # Create an apikey with the given permissions
        body = {'name': email, 'permissions': permissions}
        try:
            apikey = request.client.post('/auth/v1/apikey', body)
        except ZmlpInvalidRequestException:
            return Response(data={'detail': "Unable to create apikey."},
                            status=status.HTTP_400_BAD_REQUEST)
        encoded_apikey = encode_apikey(apikey).decode('utf-8')
        # Create a membership for given user
        Membership.objects.create(user=user, project=project, apikey=encoded_apikey)
        # Serialize the Resulting user like the Detail endpoint
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_201_CREATED)

    def update(self, request, project_pk, pk):
        # Modify the attributes of the specified user, updating the apikey & membership
        # if necessary
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)

    @transaction.atomic
    def destroy(self, request, project_pk, pk):
        # Remove the User's Membership and delete the associated apikey
        membership = self.get_object(pk, project_pk)
        apikey = membership.apikey
        # Delete Users Apikey
        try:
            key_data = decode_apikey(apikey)
        except ValueError:
            return Response(data={'detail': 'Unable to parse apikey.'},
                            status=status.HTTP_400_BAD_REQUEST)
        try:
            apikey_id = key_data['id']
        except KeyError:
            return Response(data={'detail': 'Apikey is incomplete.'},
                            status=status.HTTP_400_BAD_REQUEST)
        response = request.client.delete(f'/auth/v1/apikey/{apikey_id}')
        if not response.status_code == 200:
            return Response(data={'detail': 'Unable to delete apikey in ZMLP.'},
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        membership.delete()
        return Response(status=status.HTTP_200_OK)
