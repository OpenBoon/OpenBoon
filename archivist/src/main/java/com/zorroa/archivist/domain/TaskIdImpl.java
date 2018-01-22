package com.zorroa.archivist.domain;

import com.zorroa.cluster.thrift.TaskStartT;

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
        return taskId;
    }

    @Override
    public Integer getParentTaskId() {
        return parentId;
    }

    @Override
    public Integer getJobId() {
        return jobId;
    }
}
