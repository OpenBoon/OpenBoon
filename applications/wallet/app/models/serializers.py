from rest_framework import serializers


class ModelSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False)
    name = serializers.CharField(required=True)
    type = serializers.CharField(required=True)
    moduleName = serializers.CharField(required=False)
    fileId = serializers.CharField(required=False)
    trainingJobName = serializers.CharField(required=False)
    ready = serializers.BooleanField(required=False)
    deploySearch = serializers.JSONField(required=False)
    timeCreated = serializers.IntegerField(required=False)
    timeModified = serializers.IntegerField(required=False)
    actorCreated = serializers.CharField(required=False)
    actorModified = serializers.CharField(required=False)
    url = serializers.CharField(required=False)
    runningJobId = serializers.CharField(required=False, default='', allow_blank=True)


class ModelTypeSerializer(serializers.Serializer):
    name = serializers.CharField()
    description = serializers.CharField()
    objective = serializers.CharField()
    provider = serializers.CharField()
    deployOnTrainingSet = serializers.BooleanField()
    minConcepts = serializers.IntegerField()
    minExamples = serializers.IntegerField()


class LabelSerializer(serializers.Serializer):
    assetId = serializers.CharField(required=True)
    label = serializers.CharField(required=True)
    bbox = serializers.ListField(default=None)
    simhash = serializers.CharField(default=None)
    scope = serializers.ChoiceField(choices=['TRAIN', 'TEST'], default='TRAIN')


class AddLabelsSerializer(serializers.Serializer):
    addLabels = LabelSerializer(many=True)


class UpdateLabelsSerializer(serializers.Serializer):
    addLabels = LabelSerializer(many=True)
    removeLabels = LabelSerializer(many=True)


class RemoveLabelsSerializer(serializers.Serializer):
    removeLabels = LabelSerializer(many=True)


class RenameLabelSerializer(serializers.Serializer):
    label = serializers.CharField(required=True)
    newLabel = serializers.CharField(required=True)


class DestroyLabelSerializer(serializers.Serializer):
    label = serializers.CharField(required=True)
