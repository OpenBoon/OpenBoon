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
    running_job_id = serializers.CharField(required=False, default='', allow_blank=True)


class ModelTypeSerializer(serializers.Serializer):
    name = serializers.CharField()
    description = serializers.CharField()
    mlType = serializers.CharField()
    provider = serializers.CharField()
    runOnTrainingSet = serializers.BooleanField()


class LabelSerializer(serializers.Serializer):
    asset_id = serializers.CharField(required=True)
    label = serializers.CharField(required=True)
    bbox = serializers.ListField(default=None)
    simhash = serializers.CharField(default=None)
    scope = serializers.ChoiceField(choices=['TRAIN', 'TEST'], default='TRAIN')


class AddLabelsSerializer(serializers.Serializer):
    add_labels = LabelSerializer(many=True)


class UpdateLabelsSerializer(serializers.Serializer):
    add_labels = LabelSerializer(many=True)
    remove_labels = LabelSerializer(many=True)


class RemoveLabelsSerializer(serializers.Serializer):
    remove_labels = LabelSerializer(many=True)
