from rest_framework import serializers


class DatasetSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False, allow_null=True)
    projectId = serializers.UUIDField(required=False)
    name = serializers.CharField(required=True)
    type = serializers.CharField(required=True)
    description = serializers.CharField(required=False, allow_blank=True)
    modelCount = serializers.IntegerField(required=False)
    timeCreated = serializers.IntegerField(default=0)
    timeModified = serializers.IntegerField(default=0)
