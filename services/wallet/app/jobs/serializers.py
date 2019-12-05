from rest_framework import serializers


class MaxRunningTasksSerializer(serializers.Serializer):

    max_running_tasks = serializers.IntegerField
