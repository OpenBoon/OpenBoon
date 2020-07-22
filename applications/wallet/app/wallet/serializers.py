from django.contrib.auth.models import Group
from rest_framework import serializers


class GroupSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Group
        fields = ['url', 'name']


class PageSerializer(serializers.Serializer):
    """Used for serializing the pagination information for responses."""
    _from = serializers.IntegerField(required=True, label='from')
    size = serializers.IntegerField(required=True)
    totalCount = serializers.IntegerField(required=True)
