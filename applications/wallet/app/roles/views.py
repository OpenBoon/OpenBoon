from django.conf import settings
from rest_framework.response import Response

from projects.viewsets import BaseProjectViewSet
from roles.serializers import RoleSerializer


class RolesViewSet(BaseProjectViewSet):
    serializer_class = RoleSerializer

    def list(self, request, project_pk):
        serializer = self.get_serializer(data=settings.ROLES, many=True)
        if not serializer.is_valid():
            return Response(serializer.errors, status=500)
        return Response({'results': serializer.data})
