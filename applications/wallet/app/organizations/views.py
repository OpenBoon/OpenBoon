from django.contrib.auth import get_user_model
from django.http import JsonResponse
from rest_framework import status
from rest_framework.mixins import ListModelMixin, RetrieveModelMixin
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.viewsets import GenericViewSet

from organizations.models import Organization, User, Plan
from organizations.permissions import OrganizationOwnerPermissions
from organizations.serializers import (OrganizationSerializer,
                                       OrganizationUserListSerializer,
                                       OrganizationUserDetailSerializer,
                                       OrganizationOwnerSerializer)
from projects.models import Project, Membership
from projects.serializers import ProjectDetailSerializer
from wallet.exceptions import InvalidRequestError, NotAllowedError
from wallet.paginators import FromSizePagination

User = get_user_model()


class OrganizationViewSet(ListModelMixin, GenericViewSet):
    serializer_class = OrganizationSerializer

    def get_queryset(self):
        return Organization.objects.filter(owners=self.request.user)


class BaseOrganizationOwnerViewset(GenericViewSet):
    """
    This viewset should be used for any viewset under the /organization/<ID> urls to validate
    the user making the request is an owner of the organization.

    """
    permission_classes = [IsAuthenticated, OrganizationOwnerPermissions]

    def dispatch(self, request, *args, **kwargs):
        self.organization = Organization.objects.get(id=self.kwargs['organization_pk'])
        return super().dispatch(request, *args, **kwargs)


class OrganizationProjectViewSet(ListModelMixin, BaseOrganizationOwnerViewset):
    serializer_class = ProjectDetailSerializer
    project_limits = {Plan.ACCESS: 1,
                      Plan.BUILD: 20,
                      Plan.CUSTOM_ENTERPRISE: 0}

    def get_queryset(self):
        return Project.objects.filter(organization_id=self.organization,
                                      organization__owners=self.request.user)

    def create(self, request, *args, **kwargs):
        projects_limit = self.project_limits[self.organization.plan]
        if projects_limit and self.organization.projects.count() >= projects_limit:
            message = 'You have exceeded the number of projects allowed on your.'
            raise NotAllowedError({'detail': [message]})
        name = request.data.get('name')
        if not name:
            raise InvalidRequestError({'detail': ['"name" argument is missing.']})
        project = Project.objects.create(name=name, organization=self.organization)
        project.sync_with_zmlp()
        return Response({'id': project.id, 'name': project.name})


class OrganizationUserViewSet(ListModelMixin, RetrieveModelMixin, BaseOrganizationOwnerViewset):
    pagination_class = FromSizePagination

    def get_serializer_class(self):
        action_map = {'list': OrganizationUserListSerializer,
                      'retrieve': OrganizationUserDetailSerializer}
        return action_map[self.action]

    def get_queryset(self):
        return User.objects.filter(
            projects__organization=self.organization).distinct()

    def get_serializer_context(self):
        context = {'organization': self.organization}
        return context

    def destroy(self, request, *args, **kwargs):
        user = self.get_object()
        memberships = Membership.objects.filter(
            user=user, project__organization=self.organization)
        for membership in memberships:
            membership.delete_and_sync_with_zmlp(membership.project.get_admin_client())
        message = f'Success, removed {user.email} from {memberships.count()} projects.'
        return Response({'detail': [message]})


class OrganizationOwnerViewSet(ListModelMixin, BaseOrganizationOwnerViewset):
    serializer_class = OrganizationOwnerSerializer

    def get_queryset(self):
        return self.organization.owners.all()

    def destroy(self, request, *args, **kwargs):
        user = self.get_object()
        self.organization.owners.remove(user)
        message = f'Success, removed {user.email} as an owner of {self.organization.name}.'
        return JsonResponse({'detail': [message]})

    def create(self, request, *args, **kwargs):
        if not request.data.get('emails'):
            raise InvalidRequestError({'detail': ['"emails" argument is missing']})
        response_body = {'results': {'succeeded': [], 'failed': []}}
        for email in request.data.get('emails'):
            try:
                user = User.objects.get(email=email)
                self.organization.owners.add(user)
                response_body['results']['succeeded'].append(email)
            except User.DoesNotExist:
                response_body['results']['failed'].append(email)
        return Response(data=response_body, status=status.HTTP_207_MULTI_STATUS)
