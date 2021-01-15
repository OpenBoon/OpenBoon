from rest_framework import serializers
from metrics.records.models import ApiCall


class ApiCallSerializer(serializers.ModelSerializer):
    class Meta:
        model = ApiCall
        fields = ['id', 'project', 'service', 'asset_id', 'asset_path', 'image_count',
                  'video_minutes', 'created_date', 'modified_date']


class ReportSerializer(serializers.Serializer):
    project = serializers.UUIDField()
    service = serializers.CharField()
    image_count = serializers.IntegerField()
    video_minutes = serializers.FloatField()


class TierSerializer(serializers.Serializer):
    image_count = serializers.IntegerField()
    video_minutes = serializers.FloatField()


class TieredUsageSerializer(serializers.Serializer):
    tier_1 = TierSerializer()
    tier_2 = TierSerializer()
