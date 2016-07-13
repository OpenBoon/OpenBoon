package com.zorroa.archivist.domain;

import java.util.Map;

/**
 * A module name and args needed to create a processor spec.
 */
public class UnresolvedModule {
    private String type;
    private Map<String, Object> args;

    public UnresolvedModule() {}

    public UnresolvedModule(String type, Map<String, Object> args) {
        this.type = type;
        this.args = args;
    }
    public String getType() {
        return type;
    }

    public UnresolvedModule setType(String type) {
        this.type = type;
        return this;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public UnresolvedModule setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }
}
