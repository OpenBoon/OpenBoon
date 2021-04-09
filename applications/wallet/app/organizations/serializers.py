from django.contrib.auth import get_user_model
from rest_framework import serializers

from organizations.models import Organization
from projects.models import Membership, Project


class OrganizationSerializer(serializers.ModelSerializer):
    projectCount = serializers.SerializerMethodField('get_project_count')

    class Meta:
        model = Organization
        fields = ['id', 'name', 'plan', 'projectCount', 'createdDate', 'modifiedDate']

    def get_project_count(self, obj):
        return obj.projects.filter(isActive=True).count()


class OrganizationUserListSerializer(serializers.ModelSerializer):
    firstName = serializers.CharField(source='first_name')
    lastName = serializers.CharField(source='last_name')
    projectCount = serializers.SerializerMethodField('get_project_count')

    class Meta:
        model = get_user_model()
        fields = ['id', 'firstName', 'lastName', 'email', 'projectCount']

    def get_project_count(self, obj):
        organization = self.context.get('organization')
        return organization.projects.filter(users=obj, isActive=True).distinct().count()


class UserProjectSerializer(serializers.ModelSerializer):
    roles = serializers.SerializerMethodField()

    class Meta:
        model = Project
        fields = ['id', 'name', 'roles']

    def get_roles(self, obj):
        user_id = self.context.get('user_id')
        return Membership.objects.get(user_id=user_id, project=obj).roles
