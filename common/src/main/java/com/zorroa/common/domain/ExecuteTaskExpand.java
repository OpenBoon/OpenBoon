package com.zorroa.common.domain;

/**
 * Created by chambers on 8/23/16.
 */
public class ExecuteTaskExpand {

    private String name;
    private String script;
    private Integer parentTaskId;
    private int jobId;

    public ExecuteTaskExpand() { }

    public ExecuteTaskExpand(int jobId, Integer parentTaskId) {
        this.jobId = jobId;
        this.parentTaskId = parentTaskId;
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

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public ExecuteTaskExpand setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
        return this;
    }

    public int getJobId() {
        return jobId;
    }

    public ExecuteTaskExpand setJobId(int jobId) {
        this.jobId = jobId;
        return this;
    }
}
