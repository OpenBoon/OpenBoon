from django.contrib.auth.models import Group, User
from rest_framework import serializers


class UserSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'url', 'username', 'first_name', 'last_name', 'email', 'groups',
                  'is_active', 'is_staff', 'is_superuser', 'last_login',
                  'date_joined']


class GroupSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Group
        fields = ['url', 'name']


class PageSerializer(serializers.Serializer):
    """Used for serializing the pagination information for responses."""
    _from = serializers.IntegerField(required=True, label='from')
    size = serializers.IntegerField(required=True)
    totalCount = serializers.IntegerField(required=True)
