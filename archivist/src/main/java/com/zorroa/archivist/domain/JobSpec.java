package com.zorroa.archivist.domain;

import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * An internal class for creating a new batch job.
 */
public class JobSpec {

    private Integer jobId;
    private Map<String, Object> args;
    private Map<String, String> env;
    private String name;
    private PipelineType type;
    private String logPath;

    private List<TaskSpec> tasks;

    public JobSpec() { }

    public JobSpec(String name, PipelineType type) {
        this.name = name;
        this.type = type;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public JobSpec setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public JobSpec setEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    public JobSpec putToArgs(String k, Object v) {
        if (args == null) {
            args = Maps.newHashMap();
        }
        args.put(k, v);
        return this;
    }

    public JobSpec putToEnv(String k, String v) {
        if (env == null) {
            env = Maps.newHashMap();
        }
        env.put(k, v);
        return this;
    }

    public String getName() {
        return name;
    }

    public JobSpec setName(String name) {
        this.name = name;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public JobSpec setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public JobSpec setJobId(Integer jobId) {
        this.jobId = jobId;
        return this;
    }

    public List<TaskSpec> getTasks() {
        return tasks;
    }

    public JobSpec setTasks(List<TaskSpec> tasks) {
        this.tasks = tasks;
        return this;
    }

    public String getLogPath() {
        return logPath;
    }

    public JobSpec setLogPath(String logPath) {
        this.logPath = logPath;
        return this;
    }
}
