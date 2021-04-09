from rest_framework import serializers


class DataSourceSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=False, allow_null=True)
    name = serializers.CharField(required=True)
    uri = serializers.CharField(required=True)
    credentials = serializers.ListField(child=serializers.CharField(), default=[])
    fileTypes = serializers.ListField(child=serializers.CharField(), default=[])
    modules = serializers.ListField(child=serializers.CharField(), default=[])
    actorCreated = serializers.CharField(default="")
    actorModified = serializers.CharField(default="")
    projectId = serializers.UUIDField(default="")
    timeCreated = serializers.IntegerField(default=0)
    timeModified = serializers.IntegerField(default=0)
    link = serializers.CharField(required=False)
    jobId = serializers.CharField(required=False)


class CreateDataSourceSerializer(DataSourceSerializer):
    credentials = serializers.DictField(required=False)


class GcpCredentialSerializer(serializers.Serializer):
    type = serializers.CharField(required=True)
    service_account_json_key = serializers.CharField(required=True)


class AwsCredentialSerializer(serializers.Serializer):
    type = serializers.CharField(required=True)
    aws_access_key_id = serializers.CharField(required=True)
    aws_secret_access_key = serializers.CharField(required=True)


class AzureCredentialSerializer(serializers.Serializer):
    type = serializers.CharField(required=True)
    connection_string = serializers.CharField(required=True)
