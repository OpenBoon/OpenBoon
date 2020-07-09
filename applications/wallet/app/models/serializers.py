from rest_framework import serializers


class ModelSerializer(serializers.Serializer):
    name = serializers.CharField(required=True)
    type = serializers.CharField(required=True)


class LabelSerializer(serializers.Serializer):
    label = serializers.CharField(required=True)
