/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.common.search;

import java.util.Map;

/**
 * A script to use for queries or filters
 */
public class AssetScript {
    private String script;
    private Map<String, Object> params;
    private String type = "expression";

    public AssetScript() { }

    public AssetScript(String script, Map<String, Object> params) {
        this.script = script;
        this.params = params;
    }

    public String getScript() {
        return script;
    }

    public AssetScript setScript(String script) {
        this.script = script;
        return this;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public AssetScript setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    public String getType() {
        return type;
    }

    public AssetScript setType(String type) {
        this.type = type;
        return this;
    }
}
