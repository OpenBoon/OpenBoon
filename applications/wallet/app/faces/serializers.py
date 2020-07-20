from rest_framework import serializers


class FaceLabelSerializer(serializers.Serializer):
    bbox = serializers.ListField(required=True)
    simhash = serializers.CharField(required=True)
    label = serializers.CharField(required=True)


class UpdateFaceLabelsSerializer(serializers.Serializer):
    labels = FaceLabelSerializer(many=True)


class FaceAssetSerializer(serializers.Serializer):
    url = serializers.CharField(required=True)
