package com.zorroa.common.domain;

/**
 * Created by chambers on 8/23/16.
 */
public class ExecuteTask implements TaskId, JobId {

    private Integer taskId;
    private Integer jobId;
    private Integer parentTaskId;

    public Integer getTaskId() {
        return taskId;
    }

    public ExecuteTask setTaskId(Integer taskId) {
        this.taskId = taskId;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ExecuteTask setJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public ExecuteTask setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
        return this;
    }
}
