from rest_framework import serializers


class DatasetDetailSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False, allow_null=True)
    projectId = serializers.UUIDField(required=False)
    name = serializers.CharField(required=True)
    type = serializers.CharField(required=True)
    description = serializers.CharField(required=False, allow_blank=True)
    modelCount = serializers.IntegerField(required=False)
    timeCreated = serializers.IntegerField(default=0)
    timeModified = serializers.IntegerField(default=0)


class DatasetSerializer(DatasetDetailSerializer):
    conceptCount = serializers.SerializerMethodField(method_name='get_concept_count')

    def get_concept_count(self, dataset):
        client = self.context['request'].client
        path = f'/api/v4/datasets/{dataset["id"]}/_label_counts/'
        label_counts = client.get(path)
        return len(label_counts)


class DatasetTypeSerializer(serializers.Serializer):
    name = serializers.CharField()
    label = serializers.CharField()
    description = serializers.CharField()


class RawLabelSerializer(serializers.Serializer):
    label = serializers.CharField()
    bbox = serializers.ListField(default=None)
    simhash = serializers.CharField(default=None)
    scope = serializers.ChoiceField(choices=['TRAIN', 'TEST'], default='TRAIN')


class LabelSerializer(RawLabelSerializer):
    assetId = serializers.CharField()


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


class AddLabelsBySearchSerializer(serializers.Serializer):
    filters = serializers.JSONField()
    label = serializers.CharField()
    testRatio = serializers.FloatField()
