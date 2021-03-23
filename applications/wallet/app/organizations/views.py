from rest_framework.mixins import ListModelMixin
from rest_framework.viewsets import GenericViewSet

from organizations.models import Organization
from organizations.serializers import OrganizationSerializer
from projects.models import Project
from projects.serializers import ProjectDetailSerializer


class OrganizationViewSet(ListModelMixin, GenericViewSet):
    serializer_class = OrganizationSerializer

    def get_queryset(self):
        return Organization.objects.filter(owners=self.request.user)


class OrganizationProjectViewSet(ListModelMixin, GenericViewSet):
    serializer_class = ProjectDetailSerializer

    def get_queryset(self):
        return Project.objects.filter(organization_id=self.kwargs['organization_pk'],
                                      organization__owners=self.request.user)
