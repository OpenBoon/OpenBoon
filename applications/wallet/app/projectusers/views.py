import time
import logging
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
logger = logging.getLogger(__name__)


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
    * **PUT** _api/v1/projects/$Project_Id/users/$User_Id/_ - Replace a Users permissions
        - To modify: `{"permissions": ["$NewPermissionList"]}`
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

    def list(self, request, project_pk, client):
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

    def retrieve(self, request, project_pk, client, pk):
        # List details about the current project User
        user = self.get_object(pk, project_pk).user
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    def create(self, request, project_pk, client):
        batch = request.data.get('batch')
        if batch and request.data.get('email'):
            return Response(data={'detail': 'Batch argument provided with single creation arguments.'},  # noqa
                            status=status.HTTP_400_BAD_REQUEST)
        elif batch:
            response_body = {'results': {'succeeded': [], 'failed': []}}
            for entry in batch:
                response = self._create_project_user(request, project_pk, client, entry)
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
            return self._create_project_user(request, project_pk, client, request.data)

    def _create_project_user(self, request, project_pk, client, data):
        # Get the User and add the appropriate Membership & ApiKey
        try:
            email = data['email']
            permissions = data['permissions']
        except KeyError:
            return Response(data={'detail': 'Email and Permissions are required.'},
                            status=status.HTTP_400_BAD_REQUEST)

        # Get the current project and User
        project = self.get_project_object(project_pk)
        try:
            user = User.objects.get(username=email)
        except User.DoesNotExist:
            return Response(data={'detail': 'No user with the given email.'},
                            status=status.HTTP_404_NOT_FOUND)

        # Create an apikey with the given permissions
        body = {'name': email, 'permissions': permissions}
        try:
            apikey = client.post('/auth/v1/apikey', body)
        except ZmlpInvalidRequestException:
            return Response(data={'detail': "Unable to create apikey."},
                            status=status.HTTP_400_BAD_REQUEST)

        # Create a membership for given user
        encoded_apikey = encode_apikey(apikey).decode('utf-8')
        Membership.objects.create(user=user, project=project, apikey=encoded_apikey)

        # Serialize the Resulting user like the Detail endpoint
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_201_CREATED)

    def update(self, request, project_pk, client, pk):
        # Modify the permissions of the given user
        try:
            new_permissions = request.data['permissions']
        except KeyError:
            return Response(data={'detail': 'Permissions must be supplied.'},
                            status=status.HTTP_400_BAD_REQUEST)
        membership = self.get_object(pk, project_pk)
        email = membership.user.username
        apikey = decode_apikey(membership.apikey)
        apikey_id = apikey['id']

        # TODO: Replace Delete/Create logic when Auth Server supports PUT
        # Create new Key first and append epoch time (milli) to get a readable unique name
        body = {'name': f'{email}_{int(time.time()  * 1000)}',
                'permissions': new_permissions}
        try:
            new_apikey = client.post('/auth/v1/apikey', body)
        except ZmlpInvalidRequestException:
            return Response(data={'detail': "Unable to create apikey."},
                            status=status.HTTP_400_BAD_REQUEST)

        # Delete old key on success
        try:
            response = client.delete(f'/auth/v1/apikey/{apikey_id}')
        except ZmlpInvalidRequestException:
            return Response(data={'detail': "Unable to delete apikey."},
                            status=status.HTTP_400_BAD_REQUEST)
        if not response.status_code == 200:
            return Response(data={'detail': 'Error deleting apikey.'},
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        membership.apikey = encode_apikey(new_apikey).decode('utf-8')
        membership.save()
        serializer = self.get_serializer(membership.user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    @transaction.atomic
    def destroy(self, request, project_pk, client, pk):
        # Remove the User's Membership and delete the associated apikey
        membership = self.get_object(pk, project_pk)
        apikey = membership.apikey

        # Don't allow a User to remove themselves
        if request.user == membership.user:
            return Response(data={'detail': 'Cannot remove yourself from a project.'},
                            status=status.HTTP_403_FORBIDDEN)

        # Delete Users Apikey
        apikey_readable = True
        try:
            key_data = decode_apikey(apikey)
            apikey_id = key_data['id']
        except (ValueError, KeyError):
            logger.warning(f'Unable to decode apikey during delete for user {membership.user.id}.')
            apikey_readable = False

        if apikey_readable:
            try:
                response = client.delete(f'/auth/v1/apikey/{apikey_id}')
            except ZmlpInvalidRequestException:
                return Response(data={'detail': "Unable to delete apikey."},
                                status=status.HTTP_400_BAD_REQUEST)
            if not response.status_code == 200:
                return Response(data={'detail': 'Error deleting apikey.'},
                                status=status.HTTP_500_INTERNAL_SERVER_ERROR)

        membership.delete()
        return Response(status=status.HTTP_200_OK)
