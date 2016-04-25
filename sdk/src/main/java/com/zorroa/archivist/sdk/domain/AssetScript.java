/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.Map;

/**
 * A script to use for queries or filters
 */
public class AssetScript {
    private String script;
    private Map<String, Object> params;

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
}
