package com.zorroa.common.domain;

/**
 * Created by chambers on 8/25/16.
 */
public class ExecuteTaskResponse implements TaskId, JobId {

    private Object response;
    private ExecuteTask task;

    public ExecuteTaskResponse() { }

    public ExecuteTaskResponse(ExecuteTask task, Object response) {
        this.task = task;
        this.response = response;
    }

    public Object getResponse() {
        return response;
    }

    public ExecuteTaskResponse setResponse(Object response) {
        this.response = response;
        return this;
    }

    @Override
    public Integer getTaskId() {
        return task.getTaskId();
    }

    @Override
    public Integer getParentTaskId() {
        return task.getParentTaskId();
    }

    @Override
    public Integer getJobId() {
        return task.getJobId();
    }
}
