from rest_framework import serializers

from webhooks.models import Trigger


class WebhookSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False)
    projectId = serializers.UUIDField(required=False)
    url = serializers.URLField()
    secretKey = serializers.CharField()
    triggers = serializers.ListSerializer(child=serializers.CharField())
    active = serializers.BooleanField(required=False)
    timeCreated = serializers.IntegerField(required=False)
    timeModified = serializers.IntegerField(required=False)
    link = serializers.CharField(required=False)


class TriggerSerializer(serializers.ModelSerializer):
    class Meta:
        model = Trigger
        fields = '__all__'
