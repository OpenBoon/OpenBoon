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


class LabelSerializer(serializers.Serializer):
    assetId = serializers.CharField()
    label = serializers.CharField()
    bbox = serializers.ListField(default=None)
    simhash = serializers.CharField(default=None)
    scope = serializers.ChoiceField(choices=['TRAIN', 'TEST'], default='TRAIN')


class RemoveLabelsSerializer(serializers.Serializer):
    removeLabels = LabelSerializer(many=True)


class AddLabelsSerializer(serializers.Serializer):
    addLabels = LabelSerializer(many=True)


class UpdateLabelsSerializer(serializers.Serializer):
    addLabels = LabelSerializer(many=True)
    removeLabels = LabelSerializer(many=True)


class RenameLabelSerializer(serializers.Serializer):
    label = serializers.CharField()
    newLabel = serializers.CharField()


class DestroyLabelSerializer(serializers.Serializer):
    label = serializers.CharField()
