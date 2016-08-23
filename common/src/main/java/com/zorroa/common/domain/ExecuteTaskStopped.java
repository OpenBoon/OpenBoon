package com.zorroa.common.domain;

/**
 * The ZPS Result contains the exit status of the ZPS process.
 */
public class ExecuteTaskStopped extends ExecuteTask {

    private int exitStatus;

    public ExecuteTaskStopped() { }

    public ExecuteTaskStopped(ExecuteTask task, int exitStatus) {
        this.setTaskId(task.getTaskId());
        this.setJobId(task.getJobId());
        this.setParentTaskId(task.getParentTaskId());
        this.exitStatus = exitStatus;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public ExecuteTaskStopped setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
        return this;
    }
}
