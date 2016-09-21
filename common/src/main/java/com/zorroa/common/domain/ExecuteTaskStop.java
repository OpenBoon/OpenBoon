package com.zorroa.common.domain;

/**
 * Created by chambers on 8/22/16.
 */
public class ExecuteTaskStop implements TaskId, JobId {

    private ExecuteTask task;
    private String reason;
    private TaskState newState;

    public ExecuteTaskStop() { }

    public ExecuteTaskStop(int job, int task, int parent) {
        this.task = new ExecuteTask(job, task, parent);
    }

    public ExecuteTaskStop(ExecuteTask task) {
        this.task = task;
    }

    public String getReason() {
        return reason;
    }

    public ExecuteTaskStop setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public TaskState getNewState() {
        return newState;
    }

    public ExecuteTaskStop setNewState(TaskState newState) {
        this.newState = newState;
        return this;
    }

    public ExecuteTask getTask() {
        return task;
    }

    public ExecuteTaskStop setTask(ExecuteTask task) {
        this.task = task;
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
