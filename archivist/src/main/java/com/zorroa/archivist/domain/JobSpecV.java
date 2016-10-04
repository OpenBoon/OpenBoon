package com.zorroa.archivist.domain;

import com.google.common.collect.Maps;
import com.zorroa.sdk.zps.ZpsScript;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A validated variant of the JobSpec class used in REST endpoint.
 */
public class JobSpecV {

    /**
     * Any global args for the script.  Global args can be obtained via
     * the Context object internal to the Processor or referenced
     * with an exression.
     */
    private Map<String, Object> args;

    /**
     * Environment variables to be set on every task.
     */
    private Map<String, String> env;

    @NotEmpty
    private String name;

    @NotNull
    private PipelineType type;

    @NotNull
    private ZpsScript script;

    public JobSpecV() { }

    public JobSpecV(String name, PipelineType type) {
        this.name = name;
        this.type = type;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public JobSpecV setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public JobSpecV setEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    public JobSpecV putToArgs(String k, Object v) {
        if (args == null) {
            args = Maps.newHashMap();
        }
        args.put(k, v);
        return this;
    }

    public JobSpecV putToEnv(String k, String v) {
        if (env == null) {
            env = Maps.newHashMap();
        }
        env.put(k, v);
        return this;
    }

    public String getName() {
        return name;
    }

    public JobSpecV setName(String name) {
        this.name = name;
        return this;
    }

    public PipelineType getType() {
        return type;
    }

    public JobSpecV setType(PipelineType type) {
        this.type = type;
        return this;
    }

    public ZpsScript getScript() {
        return script;
    }

    public JobSpecV setScript(ZpsScript script) {
        this.script = script;
        return this;
    }
}
