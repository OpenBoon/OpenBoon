/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.Map;

/**
 * A script to use for queries or filters
 */
public class AssetScript {
    private String name;
    private Map<String, Object> params;

    public String getName() {
        return name;
    }

    public AssetScript setName(String name) {
        this.name = name;
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
