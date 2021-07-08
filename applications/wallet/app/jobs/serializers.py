from rest_framework import serializers

from wallet.fields import NoNullCharField


class AssetCountsSerializer(serializers.Serializer):
    assetCreatedCount = serializers.IntegerField(required=True)
    assetReplacedCount = serializers.IntegerField(required=True)
    assetWarningCount = serializers.IntegerField(required=True)
    assetErrorCount = serializers.IntegerField(required=True)
    # The views ensure assetTotalCount is set if it's missing
    assetTotalCount = serializers.IntegerField()


class TaskCountsSerializer(serializers.Serializer):
    tasksTotal = serializers.IntegerField(required=True)
    tasksWaiting = serializers.IntegerField(required=True)
    tasksRunning = serializers.IntegerField(required=True)
    tasksSuccess = serializers.IntegerField(required=True)
    tasksFailure = serializers.IntegerField(required=True)
    tasksSkipped = serializers.IntegerField(required=True)
    tasksQueued = serializers.IntegerField(required=True)


class JobActionsSerializer(serializers.Serializer):
    cancel = serializers.CharField(required=True)
    errors = serializers.CharField(required=True)
    maxRunningTasks = serializers.CharField(required=True)
    pause = serializers.CharField(required=True)
    priority = serializers.CharField(required=True)
    restart = serializers.CharField(required=True)
    resume = serializers.CharField(required=True)
    retryAllFailures = serializers.CharField(required=True)


class JobSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=True)
    projectId = serializers.CharField(default="")
    dataSourceId = NoNullCharField(default="")
    name = serializers.CharField(required=True)
    state = serializers.CharField(required=True)
    assetCounts = AssetCountsSerializer(required=True)
    taskCounts = TaskCountsSerializer(required=True)
    timeStarted = serializers.IntegerField(required=True)
    timeUpdated = serializers.IntegerField(required=True)
    timeCreated = serializers.IntegerField(required=True)
    timeStopped = serializers.IntegerField(default=0)
    priority = serializers.IntegerField(required=True)
    paused = serializers.BooleanField(required=True)
    timePauseExpired = serializers.IntegerField(required=True)
    maxRunningTasks = serializers.IntegerField(required=True)
    jobId = serializers.UUIDField(required=True)
    url = serializers.CharField(required=False)
    actions = JobActionsSerializer(required=True)
    tasks = serializers.CharField(required=True)


class StackTraceSerializer(serializers.Serializer):
    file = serializers.CharField(required=True)
    lineNumber = serializers.IntegerField(required=True)
    className = serializers.CharField(required=True)
    methodName = serializers.CharField(default='', allow_blank=True)


class TaskErrorSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=True)
    taskId = serializers.UUIDField(required=True)
    jobId = serializers.UUIDField(required=True)
    message = serializers.CharField(default="")
    assetId = serializers.CharField(default="")
    path = serializers.CharField(default="")
    processor = serializers.CharField(default="")
    fatal = serializers.BooleanField(required=True)
    analyst = serializers.CharField(default="")
    phase = serializers.CharField(default="")
    timeCreated = serializers.IntegerField(required=True)
    stackTrace = serializers.ListField(child=StackTraceSerializer(), default=[])
    url = serializers.CharField(required=False)
    jobName = serializers.CharField(default="")


class TaskActionsSerializer(serializers.Serializer):
    retry = serializers.CharField(required=True)
    assets = serializers.CharField(required=True)
    script = serializers.CharField(required=True)
    errors = serializers.CharField(required=True)
    logs = serializers.CharField(required=True)


class TaskSerializer(serializers.Serializer):
    id = serializers.UUIDField(required=True)
    jobId = serializers.UUIDField(required=True)
    projectId = serializers.CharField(default="")
    dataSourceId = NoNullCharField(default="")
    name = serializers.CharField(required=True)
    state = serializers.CharField(required=True)
    host = serializers.CharField(default='')
    timeStarted = serializers.IntegerField(required=True)
    timeCreated = serializers.IntegerField(required=True)
    timeStopped = serializers.IntegerField(required=True)
    timePing = serializers.IntegerField(required=True)
    assetCounts = AssetCountsSerializer(required=True)
    url = serializers.CharField(required=False)
    actions = TaskActionsSerializer(required=True)
