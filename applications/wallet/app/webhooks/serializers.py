from rest_framework import serializers


class WebhookSerializer(serializers.Serializer):
    id = serializers.UUIDField()
    url = serializers.URLField()
    secret_token = serializers.CharField()
    triggers = serializers.CharField(many=True)
    active = serializers.BooleanField
    timeCreated = serializers.IntegerField(required=False)
    timeUpdated = serializers.IntegerField(required=False)

