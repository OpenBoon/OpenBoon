from rest_framework import serializers

from organizations.models import Organization


class OrganizationSerializer(serializers.ModelSerializer):
    projectCount = serializers.SerializerMethodField('get_project_count')

    class Meta:
        model = Organization
        fields = ['id', 'name', 'plan', 'projectCount']

    def get_project_count(self, obj):
        return obj.projects.count()
