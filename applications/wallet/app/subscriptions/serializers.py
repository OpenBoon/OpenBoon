from rest_framework import serializers

from subscriptions.models import Subscription


class SubscriptionLimitsUsageSerializer(serializers.Serializer):
    videoHours = serializers.SerializerMethodField()
    imageCount = serializers.SerializerMethodField()

    def get_videoHours(self, obj):
        return obj['video_hours']

    def get_imageCount(self, obj):
        return obj['image_count']


class SubscriptionSerializer(serializers.HyperlinkedModelSerializer):
    url = serializers.SerializerMethodField()
    usage = SubscriptionLimitsUsageSerializer()

    class Meta:
        model = Subscription
        fields = ('id', 'project', 'tier', 'usage', 'createdDate', 'modifiedDate', 'url')

    def get_url(self, obj):
        request = self.context['request']
        current_url = request.build_absolute_uri(request.path)
        if current_url.endswith(f'subscriptions/{obj.id}/'):
            # Makes sure to return the correct URL when serialized for a detail response
            return current_url
        else:
            return f'{current_url}{obj.id}/'
