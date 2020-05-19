from rest_framework import serializers

from assets.serializers import AssetSerializer
from searches.models import Search


class SearchSerializer(serializers.ModelSerializer):
    """Serializer to return a Saved Search"""

    class Meta:
        model = Search
        fields = ('id', 'project', 'name', 'search', 'created_date', 'modified_date',
                  'created_by')

    def to_representation(self, instance):
        self.fields['created_by'] = serializers.HyperlinkedRelatedField(view_name='user-detail',
                                                                        read_only=True)
        self.fields['project'] = serializers.HyperlinkedRelatedField(view_name='project-detail',
                                                                     read_only=True)
        return super(SearchSerializer, self).to_representation(instance)


class SearchAssetSerializer(AssetSerializer):
    thumbnail_url = serializers.CharField(required=False, allow_null=True)
    asset_style = serializers.CharField(required=False, allow_null=True)
    video_length = serializers.FloatField(required=False, allow_null=True)
    video_proxy_url = serializers.CharField(required=False, allow_null=True)
