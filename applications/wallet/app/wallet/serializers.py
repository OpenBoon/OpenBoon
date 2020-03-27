from django.contrib.auth.models import Group, User
from rest_framework import serializers
from rest_framework.relations import HyperlinkedIdentityField

from projects.models import Membership


class UserSerializer(serializers.HyperlinkedModelSerializer):
    roles = serializers.SerializerMethodField()
    agreed_to_policies = serializers.SerializerMethodField()

    agreements = HyperlinkedIdentityField(
        view_name='agreement-list',
        lookup_url_kwarg='user_pk'
    )

    class Meta:
        model = User
        fields = ['id', 'url', 'username', 'first_name', 'last_name', 'email', 'groups',
                  'is_active', 'is_staff', 'is_superuser', 'last_login',
                  'date_joined', 'roles', 'agreed_to_policies', 'agreements']

    def get_roles(self, obj):
        memberships = Membership.objects.filter(user=obj)
        roles = {}
        for membership in memberships:
            roles[str(membership.project.id)] = membership.roles
        return roles

    def get_agreed_to_policies(self, obj):
        agreements = obj.agreements.order_by('-created_date')
        if len(agreements) == 0:
            return '00000000'
        latest_date = agreements[0].created_date
        return f'{latest_date.year:04}{latest_date.month:02}{latest_date.day:02}'


class GroupSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Group
        fields = ['url', 'name']


class PageSerializer(serializers.Serializer):
    """Used for serializing the pagination information for responses."""
    _from = serializers.IntegerField(required=True, label='from')
    size = serializers.IntegerField(required=True)
    totalCount = serializers.IntegerField(required=True)
