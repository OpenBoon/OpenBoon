package com.zorroa.archivist.domain;

import com.zorroa.common.domain.JobId;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;

/**
 * Created by chambers on 8/22/16.
 */
public class TaskSpec implements JobId {

    private String name;
    private Integer jobId;
    private Integer parentTaskId;
    private String script;

    public TaskSpec() { }

    public TaskSpec(int jobId, String name) {
        this.jobId = jobId;
        this.name = name;
    }

    public Integer getJobId() {
        return jobId;
    }

    public TaskSpec setJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    public String getName() {
        return name;
    }

    public TaskSpec setName(String name) {
        this.name = name;
        return this;
    }

    public String getScript() {
        return script;
    }

    public TaskSpec setScript(String script) {
        this.script = script;
        return this;
    }

    public TaskSpec setScript(ZpsScript script) {
        this.script = Json.serializeToString(script, "{}");
        return this;
    }

    public Integer getParentTaskId() {
        return parentTaskId;
    }

    public TaskSpec setParentTaskId(Integer parentTaskId) {
        this.parentTaskId = parentTaskId;
        return this;
    }
}
