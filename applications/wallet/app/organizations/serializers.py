from django.contrib.auth import get_user_model
from rest_framework import serializers

from organizations.models import Organization
from projects.models import Membership


class OrganizationSerializer(serializers.ModelSerializer):
    projectCount = serializers.SerializerMethodField('get_project_count')

    class Meta:
        model = Organization
        fields = ['id', 'name', 'plan', 'projectCount', 'createdDate', 'modifiedDate']

    def get_project_count(self, obj):
        return obj.projects.count()


class OrganizationUserListSerializer(serializers.ModelSerializer):
    firstName = serializers.CharField(source='first_name')
    lastName = serializers.CharField(source='last_name')
    projectCount = serializers.SerializerMethodField('get_project_count')

    class Meta:
        model = get_user_model()
        fields = ['id', 'firstName', 'lastName', 'email', 'projectCount']

    def get_project_count(self, obj):
        organization = self.context.get('organization')
        return organization.projects.filter(users=obj).distinct().count()


class OrganizationUserDetailSerializer(serializers.ModelSerializer):
    firstName = serializers.CharField(source='first_name')
    lastName = serializers.CharField(source='last_name')

    projects = serializers.SerializerMethodField()

    class Meta:
        model = get_user_model()
        fields = ['id', 'firstName', 'lastName', 'email', 'projects']

    def get_projects(self, obj):
        projects = []
        organization = self.context.get('organization')
        memberships = Membership.objects.filter(user=obj, project__organization=organization)
        for membership in memberships:
            projects.append({'id': membership.project_id,
                             'name': membership.project.name,
                             'roles': membership.roles})
        return projects


class OrganizationOwnerSerializer(serializers.ModelSerializer):
    firstName = serializers.CharField(source='first_name')
    lastName = serializers.CharField(source='last_name')

    class Meta:
        model = get_user_model()
        fields = ['id', 'firstName', 'lastName', 'email']
