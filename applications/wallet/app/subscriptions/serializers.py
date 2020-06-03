from rest_framework import serializers

from subscriptions.models import Subscription


class SubscriptionLimitsUsageSerializer(serializers.Serializer):
    video_hours = serializers.SerializerMethodField()
    image_count = serializers.SerializerMethodField()

    def get_video_hours(self, obj):
        return obj['video_hours']

    def get_image_count(self, obj):
        return obj['image_count']


class SubscriptionSerializer(serializers.HyperlinkedModelSerializer):
    url = serializers.SerializerMethodField()
    usage = SubscriptionLimitsUsageSerializer()

    class Meta:
        model = Subscription
        fields = ('id', 'project', 'tier', 'usage', 'created_date', 'modified_date', 'url')

    def get_url(self, obj):
        request = self.context['request']
        current_url = request.build_absolute_uri(request.path)
        if current_url.endswith(f'subscriptions/{obj.id}/'):
            # Makes sure to return the correct URL when serialized for a detail response
            return current_url
        else:
            return f'{current_url}{obj.id}/'
