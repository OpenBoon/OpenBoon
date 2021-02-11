import logging

from django.contrib.auth import get_user_model
from rest_framework import serializers
from rest_framework.relations import HyperlinkedIdentityField

from wallet.utils import convert_base64_to_json
from projects.models import Project

logger = logging.getLogger(__name__)


class ProjectSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Project
        fields = ('id', 'name', 'url', 'jobs', 'apikeys', 'assets', 'users', 'roles',
                  'permissions', 'tasks', 'taskerrors', 'datasources',
                  'modules', 'providers', 'searches', 'faces', 'visualizations',
                  'models', 'createdDate', 'modifiedDate')

    jobs = HyperlinkedIdentityField(
        view_name='job-list',
        lookup_url_kwarg='project_pk'
    )
    tasks = HyperlinkedIdentityField(
        view_name='task-list',
        lookup_url_kwarg='project_pk'
    )
    taskerrors = HyperlinkedIdentityField(
        view_name='taskerror-list',
        lookup_url_kwarg='project_pk'
    )
    apikeys = HyperlinkedIdentityField(
        view_name='apikey-list',
        lookup_url_kwarg='project_pk'
    )
    assets = HyperlinkedIdentityField(
        view_name='asset-list',
        lookup_url_kwarg='project_pk'
    )
    users = HyperlinkedIdentityField(
        view_name='projectuser-list',
        lookup_url_kwarg='project_pk'
    )
    roles = HyperlinkedIdentityField(
        view_name='role-list',
        lookup_url_kwarg='project_pk'
    )
    permissions = HyperlinkedIdentityField(
        view_name='permission-list',
        lookup_url_kwarg='project_pk',
    )
    datasources = HyperlinkedIdentityField(
        view_name='datasource-list',
        lookup_url_kwarg='project_pk',
    )
    modules = HyperlinkedIdentityField(
        view_name='module-list',
        lookup_url_kwarg='project_pk',
    )
    providers = HyperlinkedIdentityField(
        view_name='provider-list',
        lookup_url_kwarg='project_pk',
    )
    searches = HyperlinkedIdentityField(
        view_name='search-list',
        lookup_url_kwarg='project_pk',
    )
    faces = HyperlinkedIdentityField(
        view_name='face-list',
        lookup_url_kwarg='project_pk'
    )
    visualizations = HyperlinkedIdentityField(
        view_name='visualization-list',
        lookup_url_kwarg='project_pk'
    )
    models = HyperlinkedIdentityField(
        view_name='model-list',
        lookup_url_kwarg='project_pk'
    )


class ProjectUserSerializer(serializers.HyperlinkedModelSerializer):
    url = serializers.SerializerMethodField()
    permissions = serializers.SerializerMethodField()
    roles = serializers.SerializerMethodField()

    class Meta:
        model = get_user_model()
        fields = ('id', 'url', 'username', 'first_name', 'last_name', 'email', 'is_active',
                  'is_staff', 'is_superuser', 'last_login', 'date_joined', 'roles',
                  'permissions')

    def get_url(self, obj):
        request = self.context['request']
        current_url = request.build_absolute_uri(request.path)
        if current_url.endswith(f'users/{obj.id}/'):
            # Makes sure to return the correct URL when serialized for a detail response
            return current_url
        else:
            return f'{current_url}{obj.id}/'

    def get_permissions(self, obj):
        membership = self._get_membership_obj(obj)
        return self._get_decoded_permissions(membership.apikey)

    def get_roles(self, obj):
        membership = self._get_membership_obj(obj)
        return membership.roles

    def _get_membership_obj(self, obj):
        project_pk = self.context['view'].kwargs['project_pk']
        return obj.memberships.get(project=project_pk)

    def _get_decoded_permissions(self, apikey):
        try:
            key_data = convert_base64_to_json(apikey)
        except ValueError:
            # Something wrong with the json string
            logger.warning('Unable to decode apikey.')
            return []
        return key_data.get('permissions', [])
