from rest_framework import serializers


class FilterSerializer(serializers.Serializer):
    type = serializers.CharField(default='')
    processor = serializers.CharField(default='')


class OpSerializer(serializers.Serializer):
    type = serializers.CharField(required=True)
    apply = serializers.JSONField(default={})
    opFilter = FilterSerializer(default={})
    maxApplyCount = serializers.IntegerField(required=True)


class ModuleSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=True)
    projectId = serializers.UUIDField(required=False)
    name = serializers.CharField(required=True)
    description = serializers.CharField(required=True)
    provider = serializers.CharField(default='')
    category = serializers.CharField(default='')
    type = serializers.CharField(default='')
    supportedMedia = serializers.ListField(child=serializers.CharField(), default=[])
    ops = serializers.ListField(child=OpSerializer(required=True), required=True)
    timeCreated = serializers.IntegerField(required=True)
    timeModified = serializers.IntegerField(required=True)
    actorCreated = serializers.CharField(required=True)
    actorModified = serializers.CharField(required=True)


class CategorySerializer(serializers.Serializer):
    name = serializers.CharField(required=True)
    modules = serializers.ListField(child=ModuleSerializer())


class ProviderSerializer(serializers.Serializer):
    name = serializers.CharField(required=True)
    logo = serializers.CharField(required=True)
    description = serializers.CharField(required=True)
    categories = serializers.ListField(child=CategorySerializer())
    sort_index = serializers.IntegerField()
