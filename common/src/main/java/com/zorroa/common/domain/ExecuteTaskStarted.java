package com.zorroa.common.domain;

/**
 * Created by chambers on 8/23/16.
 */
public class ExecuteTaskStarted {

    private ExecuteTask task;

    public ExecuteTaskStarted() { }

    public ExecuteTaskStarted(ExecuteTask task) {
        this.task = task;
    }

    public ExecuteTask getTask() {
        return task;
    }

    public ExecuteTaskStarted setTask(ExecuteTask task) {
        this.task = task;
        return this;
    }
}
