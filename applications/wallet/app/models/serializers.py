from rest_framework import serializers


class SimpleModelSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False)
    name = serializers.CharField()
    type = serializers.CharField()


class ModelSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False)
    name = serializers.CharField()
    type = serializers.CharField()
    moduleName = serializers.CharField(required=False)
    fileId = serializers.CharField(required=False)
    trainingJobName = serializers.CharField(required=False)
    unappliedChanges = serializers.BooleanField(required=False)
    applySearch = serializers.JSONField(required=False)
    timeCreated = serializers.IntegerField(required=False)
    timeModified = serializers.IntegerField(required=False)
    actorCreated = serializers.CharField(required=False)
    actorModified = serializers.CharField(required=False)
    link = serializers.CharField(required=False)
    projectId = serializers.CharField(required=False)
    ready = serializers.BooleanField(required=False)
    datasetId = serializers.CharField(required=False, allow_null=True)


class ModelTypeRestrictionsSerializer(serializers.Serializer):
    requiredLabels = serializers.IntegerField()
    missingLabels = serializers.IntegerField()
    requiredAssetsPerLabel = serializers.IntegerField()
    missingLabelsOnAssets = serializers.IntegerField()


class ModelDetailSerializer(ModelSerializer):
    runningJobId = serializers.CharField(required=False, default='', allow_blank=True)
    modelTypeRestrictions = ModelTypeRestrictionsSerializer()
    timeLastApplied = serializers.IntegerField(required=False, allow_null=True)
    timeLastTested = serializers.IntegerField(required=False, allow_null=True)
    timeLastTrained = serializers.IntegerField(required=False, allow_null=True)
    datasetType = serializers.SerializerMethodField('get_dataset_type')

    def get_dataset_type(self, obj):
        model_types = self.context['request'].client.get('/api/v3/models/_types')
        for model_type in model_types:
            if model_type['name'] == obj['type']:
                return model_type['datasetType']
        return ''


class ModelUpdateSerializer(serializers.Serializer):
    name = serializers.CharField()
    datasetId = serializers.CharField(allow_null=True)


class ModelTypeSerializer(serializers.Serializer):
    name = serializers.CharField()
    label = serializers.CharField()
    description = serializers.CharField()
    objective = serializers.CharField()
    provider = serializers.CharField()
    deployOnTrainingSet = serializers.BooleanField()
    minConcepts = serializers.IntegerField()
    minExamples = serializers.IntegerField()
    datasetType = serializers.CharField()


class ConfusionMatrixSerializer(serializers.Serializer):
    name = serializers.CharField()
    moduleName = serializers.CharField()
    datasetId = serializers.CharField(required=False, allow_null=True)
    labels = serializers.ListField(child=serializers.CharField())
    matrix = serializers.ListField(child=serializers.ListField(child=serializers.IntegerField()))
    maxScore = serializers.FloatField()
    minScore = serializers.FloatField()
    overallAccuracy = serializers.FloatField()
    testSetOnly = serializers.BooleanField()
    isMatrixApplicable = serializers.BooleanField()
