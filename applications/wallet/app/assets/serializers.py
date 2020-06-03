from rest_framework import serializers

from wallet.serializers import PageSerializer


class AssetSerializer(serializers.Serializer):
    id = serializers.CharField(required=False, allow_null=True)
    fullscreen_url = serializers.CharField(required=False)
    metadata = serializers.DictField(required=True)


class AssetsSerializer(serializers.Serializer):
    list = AssetSerializer(many=True, required=True)
    page = PageSerializer(required=True)
