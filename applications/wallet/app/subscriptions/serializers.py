from rest_framework import serializers

from subscriptions.models import Subscription


class SubscriptionSerializer(serializers.HyperlinkedModelSerializer):
    url = serializers.SerializerMethodField()

    class Meta:
        model = Subscription
        fields = ('id', 'project', 'video_hours_limit', 'image_count_limit',
                  'modules', 'created_date', 'modified_date', 'url')

    def get_url(self, obj):
        request = self.context['request']
        current_url = request.build_absolute_uri(request.path)
        if current_url.endswith(f'subscriptions/{obj.id}/'):
            # Makes sure to return the correct URL when serialized for a detail response
            return current_url
        else:
            return f'{current_url}{obj.id}/'
