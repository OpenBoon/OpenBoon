from rest_framework import serializers


class DataSourceSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False, allow_null=True)
    name = serializers.CharField(required=True)
    uri = serializers.CharField(required=True)
    credentials = serializers.ListField(child=serializers.CharField(), default=[])
    fileTypes = serializers.ListField(child=serializers.CharField(), default=[])
    modules = serializers.ListField(child=serializers.CharField(), default=[])
    actorCreated = serializers.CharField(default="")
    actorModified = serializers.CharField(default="")
    projectId = serializers.UUIDField(default="")
    timeCreated = serializers.IntegerField(default=0)
    timeModified = serializers.IntegerField(default=0)
    url = serializers.CharField(required=False)
