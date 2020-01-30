import json
import base64
from binascii import Error as BinAsciiError
from django.contrib.auth import get_user_model
from rest_framework import serializers


class ProjectUserSerializer(serializers.HyperlinkedModelSerializer):
    url = serializers.SerializerMethodField()
    permissions = serializers.SerializerMethodField()

    class Meta:
        model = get_user_model()
        fields = ('id', 'url', 'username', 'first_name', 'last_name', 'email', 'is_active',
                  'is_staff', 'is_superuser', 'last_login', 'date_joined', 'permissions')

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

    def _get_membership_obj(self, obj):
        project_pk = self.context['view'].kwargs['project_pk']
        return obj.memberships.get(project=project_pk)

    def _get_decoded_permissions(self, apikey):
        try:
            decoded = base64.b64decode(apikey)
        except BinAsciiError:
            # Let's pray it's already a json string
            decoded = apikey
        try:
            apikey_json = json.loads(decoded)
        except json.decoder.JSONDecodeError:
            # Something wrong with the json string
            return 'Could not parse apikey, please check.'
        return apikey_json['permissions']
