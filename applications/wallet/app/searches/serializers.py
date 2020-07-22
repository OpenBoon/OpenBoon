from rest_framework import serializers

from assets.serializers import AssetSerializer
from searches.models import Search


class SearchSerializer(serializers.ModelSerializer):
    """Serializer to return a Saved Search"""

    class Meta:
        model = Search
        fields = ('id', 'project', 'name', 'search', 'created_date', 'modified_date',
                  'created_by')


class SearchAssetSerializer(AssetSerializer):
    thumbnail_url = serializers.CharField(required=False, allow_null=True)
    asset_style = serializers.CharField(required=False, allow_null=True)
    video_length = serializers.FloatField(required=False, allow_null=True)
    video_proxy_url = serializers.CharField(required=False, allow_null=True)
