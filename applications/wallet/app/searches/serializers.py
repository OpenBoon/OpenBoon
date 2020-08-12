from rest_framework import serializers

from assets.serializers import AssetSerializer
from searches.models import Search


class SearchSerializer(serializers.ModelSerializer):
    """Serializer to return a Saved Search"""

    class Meta:
        model = Search
        fields = ('id', 'project', 'name', 'search', 'createdDate', 'modifiedDate',
                  'createdBy')


class SearchAssetSerializer(AssetSerializer):
    thumbnailUrl = serializers.CharField(required=False, allow_null=True)
    assetStyle = serializers.CharField(required=False, allow_null=True)
    videoLength = serializers.FloatField(required=False, allow_null=True)
    videoProxyUrl = serializers.CharField(required=False, allow_null=True)
