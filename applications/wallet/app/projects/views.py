import logging

from django.contrib.auth import get_user_model
from django.db import transaction
from django.db.models import Q
from django.http import Http404
from rest_framework import status
from rest_framework.exceptions import PermissionDenied
from rest_framework.mixins import RetrieveModelMixin, ListModelMixin
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.viewsets import GenericViewSet

from projects.models import Membership, Project
from projects.permissions import ManagerUserPermissions
from projects.serializers import ProjectSerializer, ProjectUserSerializer, \
    ProjectDetailSerializer
from projects.viewsets import BaseProjectViewSet
from wallet.exceptions import InvalidRequestError
from wallet.paginators import FromSizePagination

logger = logging.getLogger(__name__)
User = get_user_model()


class ProjectViewSet(ListModelMixin,
                     RetrieveModelMixin,
                     GenericViewSet,
                     BaseProjectViewSet):
    """API endpoint that allows Projects to be viewed and created."""
    project_pk_kwarg = 'pk'

    def get_queryset(self):
        user = self.request.user
        return Project.objects.filter(Q(users=user) | Q(organization__owners=user)).distinct()

    def get_serializer_class(self):
        action_map = {'list': ProjectSerializer,
                      'retrieve': ProjectDetailSerializer}
        return action_map[self.action]

    def destroy(self, request, *args, **kwargs):
        project = self.get_object()
        if not project.organization.owners.filter(id=request.user.id).exists():
            raise PermissionDenied({'detail': 'You must be an owner of project to delete it.'})
        project.isActive = False
        project.save()
        project.sync_with_zmlp()
        return Response({'detail': [f'Success, Project "{project.name}" has been deleted.']})


class ProjectUserViewSet(BaseProjectViewSet):
    """Users who are Members of this Project.

    Available HTTP methods, endpoints, and what they do:

    * **GET** _api/v1/projects/$Project_Id/users/_ - List the Users who are members of $Project_Id
    * **GET** _api/v1/projects/$Project_Id/users/$User_Id/_ - Detail info on $User_Id
    * **POST** _api/v1/projects/$Project_Id/users/_ - Create membership/s to $Project_Id
        - To create one: `{"email": "user@email.com", "roles": ["ML_Tools"]}`
        - To create multiple: `{"batch": [
                                    {"email": "user@email.com", "roles": ["API_Key"]},
                                    {"email": "user2@email.com", "roles": ["User_Admin"]}
                                ]}`
    * **PUT** _api/v1/projects/$Project_Id/users/$User_Id/_ - Replace a Users Roles
        - To modify: `{"roles": ["$NewRoleList"]}`
    * **DELETE** _api/v1/projects/$Project_Id/users/$User_Id/_ - Remove $User_Id from $Project_Id

    """
    pagination_class = FromSizePagination
    serializer_class = ProjectUserSerializer
    permission_classes = [IsAuthenticated, ManagerUserPermissions]

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
        users = User.objects.filter(Q(memberships__project=project) | Q(organizations=project.organization)).distinct()
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
            raise InvalidRequestError({'detail': ['Batch argument provided with single creation arguments.']})
        elif batch:
            response_body = {'results': {'succeeded': [], 'failed': []}}
            for entry in batch:
                response = self._create_project_user(request, project_pk, entry)
                content = {'email': entry.get('email'),
                           'roles': entry.get('roles'),
                           'statusCode': response.status_code,
                           'body': response.data}
                if response.status_code in [status.HTTP_201_CREATED, status.HTTP_200_OK]:
                    response_body['results']['succeeded'].append(content)
                else:
                    response_body['results']['failed'].append(content)
            return Response(data=response_body, status=status.HTTP_207_MULTI_STATUS)
        else:
            return self._create_project_user(request, project_pk, request.data)

    def update(self, request, project_pk, pk):
        # Modify the permissions of the given user
        try:
            new_roles = request.data['roles']
        except KeyError:
            return Response(data={'detail': ['Roles must be supplied.']},
                            status=status.HTTP_400_BAD_REQUEST)
        membership = self.get_object(pk, project_pk)
        if membership.roles != new_roles:
            membership.roles = new_roles
            try:
                membership.sync_with_zmlp(request.client)
            except IOError:
                return Response(data={'detail': ['Error deleting apikey.']},
                                status=status.HTTP_500_INTERNAL_SERVER_ERROR)
            except ValueError:
                return Response(data={'detail': ['Unable to modify the admin key.']},
                                status=status.HTTP_400_BAD_REQUEST)
        serializer = self.get_serializer(membership.user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    @transaction.atomic
    def destroy(self, request, project_pk, pk):
        # Remove the User's Membership and delete the associated apikey
        membership = self.get_object(pk, project_pk)

        # Don't allow a User to remove themselves
        if request.user == membership.user:
            return Response(data={'detail': ['Cannot remove yourself from a project.']},
                            status=status.HTTP_403_FORBIDDEN)

        try:
            membership.delete_and_sync_with_zmlp(request.client)
        except IOError:
            return Response(data={'detail': ['Error deleting apikey.']},
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        return Response(status=status.HTTP_200_OK)

    def _create_project_user(self, request, project_pk, data):
        """Creates project user by generating an api key in zmlp and creating a new
        Membership.

        """
        # Get the User and add the appropriate Membership & ApiKey
        try:
            email = data['email']
            requested_roles = data['roles']
        except KeyError:
            return Response(data={'detail': ['Email and Roles are required.']},
                            status=status.HTTP_400_BAD_REQUEST)

        # Get the current project and User
        project = self.get_project_object(project_pk)
        try:
            user = User.objects.get(username=email)
        except User.DoesNotExist:
            return Response(data={'detail': ['No user with the given email.']},
                            status=status.HTTP_404_NOT_FOUND)

        # If the membership already exists return the correct status code.
        try:
            membership = user.memberships.get(project=project)
            if membership.roles == requested_roles:
                serializer = self.get_serializer(user, context={'request': request})
                return Response(data=serializer.data, status=status.HTTP_200_OK)
            else:
                return Response({'detail': ['This user already exists in this project '
                                            'with different permissions.']}, status=409)
        except Membership.DoesNotExist:
            pass

        # Create a membership for given user
        membership = Membership(user=user, project=project, roles=requested_roles)
        membership.sync_with_zmlp(client=request.client)

        # Serialize the Resulting user like the Detail endpoint
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_201_CREATED)
