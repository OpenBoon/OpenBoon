import math
from rest_framework import serializers
from metrics.records.models import ApiCall


class ApiCallSerializer(serializers.ModelSerializer):
    class Meta:
        model = ApiCall
        fields = ['id', 'project', 'service', 'asset_id', 'asset_path', 'image_count',
                  'video_seconds', 'created_date', 'modified_date']
        validators = []


class ReportSerializer(serializers.Serializer):
    project = serializers.UUIDField()
    service = serializers.CharField()
    tier = serializers.SerializerMethodField()
    image_count = serializers.IntegerField()
    video_seconds = serializers.FloatField()
    video_minutes = serializers.SerializerMethodField()
    video_hours = serializers.SerializerMethodField()

    def get_tier(self, obj):
        service = obj['service']
        if service in ApiCall.tier_2_modules:
            return 'tier_2'
        elif service in ApiCall.tier_1_modules:
            return 'tier_1'
        else:
            return 'free'

    def get_video_minutes(self, obj):
        video_seconds = obj['video_seconds']
        return video_seconds / 60.0

    def get_video_hours(self, obj):
        video_seconds = obj['video_seconds']
        return video_seconds / 60.0 / 60.0


class TierSerializer(serializers.Serializer):
    image_count = serializers.IntegerField()
    video_seconds = serializers.FloatField()
    video_minutes = serializers.SerializerMethodField()
    video_hours = serializers.SerializerMethodField()

    def get_video_minutes(self, obj):
        video_seconds = obj['video_seconds']
        return math.ceil(video_seconds / 60.0)

    def get_video_hours(self, obj):
        video_seconds = obj['video_seconds']
        return math.ceil(video_seconds / 60.0 / 60.0)


class TieredUsageSerializer(serializers.Serializer):
    tier_1 = TierSerializer()
    tier_2 = TierSerializer()
