from rest_framework import serializers
from rest_framework.relations import HyperlinkedIdentityField

from projects.models import Project


class ProjectSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Project
        fields = ('id', 'name', 'url', 'jobs', 'users')

    jobs = HyperlinkedIdentityField(
        view_name='job-list',
        lookup_url_kwarg='project_pk'
    )
