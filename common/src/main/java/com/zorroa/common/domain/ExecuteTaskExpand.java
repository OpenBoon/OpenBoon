package com.zorroa.common.domain;

/**
 * Created by chambers on 8/23/16.
 */
public class ExecuteTaskExpand extends ExecuteTask {

    private String name;
    private String script;

    public ExecuteTaskExpand() { }

    public ExecuteTaskExpand(ExecuteTask task) {
        this.setJobId(task.getJobId());
        this.setTaskId(task.getTaskId());
        this.setParentTaskId(task.getParentTaskId());
    }

    public String getName() {
        return name;
    }

    public ExecuteTaskExpand setName(String name) {
        this.name = name;
        return this;
    }

    public String getScript() {
        return script;
    }

    public ExecuteTaskExpand setScript(String script) {
        this.script = script;
        return this;
    }
}
