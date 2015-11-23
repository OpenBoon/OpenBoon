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

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
