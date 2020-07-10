from rest_framework import serializers


class ApikeySerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False, allow_null=True)
    name = serializers.CharField(required=True)
    projectId = serializers.UUIDField(required=False, allow_null=True)
    accessKey = serializers.CharField(required=False, allow_blank=True)
    secretKey = serializers.CharField(required=False, allow_blank=True)
    internal = serializers.BooleanField(required=False, allow_null=True)
    permissions = serializers.ListField(child=serializers.CharField(), required=True)
    url = serializers.SerializerMethodField()

    def get_url(self, obj):
        request = self.context['request']
        current_url = request.build_absolute_uri(request.path)
        return f'{current_url}{obj["id"]}/'
