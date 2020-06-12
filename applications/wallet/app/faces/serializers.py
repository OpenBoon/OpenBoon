from rest_framework import serializers


class LabelSerializer(serializers.Serializer):
    bbox = serializers.ListField(required=True)
    simhash = serializers.CharField(required=True)
    label = serializers.CharField(required=True)


class UpdateLabelsSerializer(serializers.Serializer):
    labels = LabelSerializer(many=True)
