package com.zorroa.common.domain;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Created by chambers on 8/22/16.
 */
public class ExecuteTaskStart extends ExecuteTask implements TaskId, JobId {

    private Map<String, String> env;
    private Map<String, Object> args;
    private String script;

    public ExecuteTaskStart() { }

    public ExecuteTaskStart(Integer jobId, Integer taskId, Integer parentTaskId) {
        this.setJobId(jobId);
        this.setTaskId(taskId);
        this.setParentTaskId(parentTaskId);
        this.env = Maps.newHashMap();
        this.args = Maps.newHashMap();
    }

    public String getScript() {
        return script;
    }

    public ExecuteTaskStart setScript(String script) {
        this.script = script;
        return this;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public ExecuteTaskStart setEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public ExecuteTaskStart setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    public ExecuteTaskStart putToEnv(String k, String v) {
        if (env == null) {
            env = Maps.newHashMap();
        }
        env.put(k, v);
        return this;
    }

    public ExecuteTaskStart putToArg(String k, Object v) {
        if (args == null) {
            args = Maps.newHashMap();
        }
        args.put(k, v);
        return this;
    }
}
