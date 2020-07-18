from django.contrib.auth.models import Group, User
from rest_framework import serializers
from rest_framework.relations import HyperlinkedIdentityField

from projects.models import Membership


class UserSerializer(serializers.HyperlinkedModelSerializer):
    roles = serializers.SerializerMethodField()
    agreed_to_policies_date = serializers.SerializerMethodField()

    agreements = HyperlinkedIdentityField(
        view_name='agreement-list',
        lookup_url_kwarg='user_pk'
    )

    class Meta:
        model = User
        fields = ['id', 'url', 'username', 'first_name', 'last_name', 'email', 'groups',
                  'is_active', 'is_staff', 'is_superuser', 'last_login',
                  'date_joined', 'roles', 'agreed_to_policies_date', 'agreements']

    def get_roles(self, obj):
        memberships = Membership.objects.filter(user=obj)
        roles = {}
        for membership in memberships:
            roles[str(membership.project.id)] = membership.roles
        return roles

    def get_agreed_to_policies_date(self, obj):
        agreements = obj.agreements.order_by('-created_date')
        if len(agreements) == 0:
            return '00000000'
        return agreements[0].policies_date


class PageSerializer(serializers.Serializer):
    """Used for serializing the pagination information for responses."""
    _from = serializers.IntegerField(required=True, label='from')
    size = serializers.IntegerField(required=True)
    totalCount = serializers.IntegerField(required=True)
