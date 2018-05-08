package com.zorroa.archivist.domain;

import com.zorroa.cluster.thrift.TaskStartT;
import com.zorroa.sdk.util.StringUtils;

import java.util.UUID;

public class TaskIdImpl implements TaskId {

    private UUID taskId;
    private UUID parentId;
    private UUID jobId;

    public TaskIdImpl(TaskStartT taskT) {
        this.taskId = UUID.fromString(taskT.id);
        this.jobId = UUID.fromString(taskT.jobId);
        this.parentId = StringUtils.uuid(taskT.parent);
    }

    @Override
    public UUID getTaskId() {
        return taskId;
    }

    @Override
    public UUID getParentTaskId() {
        return parentId;
    }

    @Override
    public UUID getJobId() {
        return jobId;
    }
}
