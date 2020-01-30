from rest_framework import status
from rest_framework.response import Response
from django.contrib.auth import get_user_model

from projects.views import BaseProjectViewSet
from projects.models import Project
from projectusers.serializers import ProjectUserSerializer
from wallet.paginators import FromSizePagination

User = get_user_model()


class ProjectUserViewSet(BaseProjectViewSet):

    ZMLP_ONLY = True
    pagination_class = FromSizePagination
    serializer_class = ProjectUserSerializer

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
        try:
            user = User.objects.get(id=pk)
        except User.DoesNotExist:
            return Response('The specified user does not exist or is not a part of this '
                            'project.',
                            status=status.HTTP_404_NOT_FOUND)
        try:
            user.projects.get(id=project_pk)
        except Project.DoesNotExist:
            return Response('The specified user does not exist or is not a part of this '
                            'project.',
                            status=status.HTTP_404_NOT_FOUND)
        serializer = self.get_serializer(user, context={'request': request})
        return Response(data=serializer.data, status=status.HTTP_200_OK)

    def create(self, request, project_pk, client):
        # Get the User and add the  appropriate Membership & ApiKey
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)

    def update(self, request, project_pk, client, *args, **kwargs):
        # Modify the attributes of the specified user, updating the apikey & membership
        # if necessary
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)

    def destroy(self, request, project_pk, client):
        # Remove the User's Membership and delete the associated apikey
        return Response(status=status.HTTP_405_METHOD_NOT_ALLOWED)
