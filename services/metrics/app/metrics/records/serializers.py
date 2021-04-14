from rest_framework import serializers
from metrics.records.models import ApiCall


class ApiCallSerializer(serializers.ModelSerializer):
    class Meta:
        model = ApiCall
        fields = ['id', 'project', 'service', 'asset_id', 'asset_path', 'image_count',
                  'video_minutes', 'created_date', 'modified_date']
        validators = []


class ReportSerializer(serializers.Serializer):
    project = serializers.UUIDField()
    service = serializers.CharField()
    tier = serializers.SerializerMethodField()
    image_count = serializers.IntegerField()
    video_minutes = serializers.FloatField()

    def get_tier(self, obj):
        service = obj['service']
        if service in ApiCall.tier_2_modules:
            return 'tier_2'
        elif service in ApiCall.tier_1_modules:
            return 'tier_1'
        else:
            return 'free'


class TierSerializer(serializers.Serializer):
    image_count = serializers.IntegerField()
    video_minutes = serializers.FloatField()


class TieredUsageSerializer(serializers.Serializer):
    tier_1 = TierSerializer()
    tier_2 = TierSerializer()
