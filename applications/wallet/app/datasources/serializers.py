from rest_framework import serializers


class DataSourceSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False, allow_null=True)
    name = serializers.CharField(required=True)
    uri = serializers.CharField(required=True)
    credential = serializers.CharField(required=False, allow_null=True, allow_blank=True)
    file_types = serializers.ListField(child=serializers.CharField(), required=True)
    modules = serializers.ListField(child=serializers.CharField(), required=True)
