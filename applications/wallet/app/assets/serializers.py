from rest_framework import serializers

from wallet.serializers import PageSerializer


class AssetSerializer(serializers.Serializer):
    id = serializers.CharField(required=False, allow_null=True)
    metadata = serializers.DictField(required=True)


class AssetsSerializer(serializers.Serializer):
    list = AssetSerializer(many=True, required=True)
    page = PageSerializer(required=True)
