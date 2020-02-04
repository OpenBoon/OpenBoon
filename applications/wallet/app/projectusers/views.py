from rest_framework import status
from rest_framework.response import Response
from django.contrib.auth import get_user_model
from django.http import Http404
from django.db import transaction

from projects.views import BaseProjectViewSet
from projects.models import Project, Membership
from projectusers.serializers import ProjectUserSerializer
from wallet.paginators import FromSizePagination
from apikeys.utils import decode_apikey

User = get_user_model()


class ProjectUserViewSet(BaseProjectViewSet):
    """Users who are Members of this Project.

    Available HTTP methods, endpoints, and what they do:

    * **GET** _api/v1/projects/$Project_Id/users/_ - List the Users who are members of $Project_Id
    * **GET** _api/v1/projects/$Project_Id/users/$User_Id/_ - Detail info on $User_Id
    * **POST** _api/v1/projects/$Project_Id/users/_ - Create a membership to $Project_Id
        - Ex. Post Body: `{"email": "user@email.com", "permissions": ["AssetsRead"]}`
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

    def list(self, request, project_pk, client):
        # Need to handle pagination
        # If the project doesn't exist or user is not a member a 403 is returned
        project = Project.objects.get(id=project_pk)
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
        # Get the User and add the  appropriate Membership & ApiKey
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)

    def update(self, request, project_pk, client, *args, **kwargs):
        # Modify the attributes of the specified user, updating the apikey & membership
        # if necessary
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)

    @transaction.atomic
    def destroy(self, request, project_pk, client, pk):
        # Remove the User's Membership and delete the associated apikey
        membership = self.get_object(pk, project_pk)
        apikey = membership.apikey
        # Delete Users Apikey
        try:
            key_data = decode_apikey(apikey)
        except ValueError:
            return Response('Unable to parse apikey.', status=status.HTTP_400_BAD_REQUEST)
        try:
            apikey_id = key_data['id']
        except KeyError:
            return Response('Apikey is incomplete.', status=status.HTTP_400_BAD_REQUEST)
        response = client.delete(f'/auth/v1/apikey/{apikey_id}')
        if not response.status_code == 200:
            return Response('Unable to delete apikey in ZMLP.',
                            status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        membership.delete()
        return Response(status=status.HTTP_200_OK)
