package com.zorroa.common.domain;

/**
 * Created by chambers on 8/23/16.
 */
public class ExecuteTaskStarted extends ExecuteTask {

    public ExecuteTaskStarted() { }

    public ExecuteTaskStarted(ExecuteTask t) {
        this.setJobId(t.getJobId());
        this.setTaskId(t.getTaskId());
        this.setParentTaskId(t.getParentTaskId());
    }
}
