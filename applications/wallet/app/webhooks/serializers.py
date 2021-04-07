from rest_framework import serializers


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
