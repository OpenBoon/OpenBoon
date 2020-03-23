from rest_framework import serializers


class FilterSerializer(serializers.Serializer):
    type = serializers.CharField(required=True)
    processor = serializers.CharField(default='')


class OpSerializer(serializers.Serializer):
    type = serializers.CharField(required=True)
    apply = serializers.JSONField(default={})
    filter = FilterSerializer(default={})
    maxApplyCount = serializers.IntegerField(required=True)


class ModuleSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=True)
    name = serializers.CharField(required=True)
    description = serializers.CharField(required=True)
    provider = serializers.CharField(default='')
    category = serializers.CharField(default='')
    supportedMedia = serializers.ListField(child=serializers.CharField(), default=[])
    restricted = serializers.BooleanField(required=True)
    ops = serializers.ListField(child=OpSerializer(required=True), required=True)
    timeCreated = serializers.IntegerField(required=True)
    timeModified = serializers.IntegerField(required=True)
    actorCreated = serializers.CharField(required=True)
    actorModified = serializers.CharField(required=True)
