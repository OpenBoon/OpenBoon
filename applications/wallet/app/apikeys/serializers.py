from rest_framework import serializers


class ApikeySerializer(serializers.Serializer):
    keyId = serializers.UUIDField(required=False, allow_null=True)
    name = serializers.CharField(required=True)
    projectId = serializers.UUIDField(required=False, allow_null=True)
    sharedKey = serializers.CharField(required=False, allow_blank=True)
    permissions = serializers.ListField(child=serializers.CharField(), required=True)
    url = serializers.SerializerMethodField()

    def get_url(self, obj):
        request = self.context['request']
        current_url = request.build_absolute_uri(request.path)
        return f'{current_url}{obj["keyId"]}/'
