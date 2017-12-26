package com.zorroa.archivist.domain;

import com.zorroa.common.cluster.thrift.TaskStartT;

public class TaskIdImpl implements TaskId {

    private Integer taskId;
    private Integer parentId;
    private Integer jobId;

    public TaskIdImpl(TaskStartT taskT) {
        this.taskId = taskT.id;
        this.jobId = taskT.jobId;
        this.parentId = taskT.parent;
    }

    @Override
    public Integer getTaskId() {
        return null;
    }

    @Override
    public Integer getParentTaskId() {
        return null;
    }

    @Override
    public Integer getJobId() {
        return null;
    }
}
