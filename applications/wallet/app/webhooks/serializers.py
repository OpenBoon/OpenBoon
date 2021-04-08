from rest_framework import serializers

from webhooks.models import Trigger


class WebhookSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    url = serializers.URLField()
    secret_token = serializers.CharField()
    triggers = serializers.ListSerializer(child=serializers.CharField())
    active = serializers.BooleanField
    timeCreated = serializers.IntegerField(required=False)
    timeUpdated = serializers.IntegerField(required=False)


class WebhookTestSerializer(serializers.Serializer):
    trigger = serializers.CharField()
    url = serializers.URLField()


class TriggerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Trigger
        fields = '__all__'
