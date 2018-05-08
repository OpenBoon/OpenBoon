package com.zorroa.cluster.zps;

import com.google.common.collect.Maps;
import com.zorroa.sdk.util.FileUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Created by chambers on 3/23/17.
 */
public class ZpsTask {
    private UUID id;
    private Map<String, String> env;
    private Map<String, Object> args;
    private String scriptPath;
    private String logPath;
    private String workPath;
    private String currentScript;

    public ZpsTask() {
        this.env = Maps.newHashMap();
        this.args = Maps.newHashMap();
    }

    public ZpsTask(String scriptPath) {
        this();
        setScriptPath(scriptPath);
    }

    public ZpsTask(String scriptPath, String logPath) {
        this();
        setScriptPath(scriptPath);
        setLogPath(logPath);
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public ZpsTask setEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public ZpsTask setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public ZpsTask setScriptPath(String scriptPath) {
        this.scriptPath = FileUtils.normalize(scriptPath);
        return this;
    }

    public String getLogPath() {
        return logPath;
    }

    public ZpsTask setLogPath(String logPath) {
        if (logPath != null) {
            this.logPath = FileUtils.normalize(logPath);
        }
        return this;
    }

    public UUID getId() {
        return id;
    }

    public ZpsTask setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getCurrentScript() {
        return currentScript;
    }

    public ZpsTask setCurrentScript(String currentScript) {
        this.currentScript = currentScript;
        return this;
    }

    public String getWorkPath() {
        return workPath;
    }

    public ZpsTask setWorkPath(String workPath) {
        this.workPath = workPath;
        return this;
    }
}
