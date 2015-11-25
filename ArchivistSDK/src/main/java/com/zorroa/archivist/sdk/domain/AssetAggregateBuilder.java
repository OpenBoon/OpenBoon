/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Search with optional aggregation
 */
public class AssetAggregateBuilder {
    private String name;
    private AssetSearch search;
    private String field;
    private int size;
    private String script;
    private Map<String, Object> scriptParams;
    private String excludeRegex;
    private String includeRegex;

    public AssetSearch getSearch() {
        return search;
    }

    public Map<String, Object> getAggregations() {
        Map<String, Object> terms = new HashMap<>();
        terms.put("size", size);
        if (field != null) {
            terms.put("field", field);
        }
        if (script != null) {
            terms.put("script", script);
            terms.put("lang", "native");
            terms.put("params", scriptParams);
        }
        if (excludeRegex != null) {
            terms.put("exclude", excludeRegex);
        }
        if (includeRegex != null) {
            terms.put("include", includeRegex);
        }
        Map<String, Object> names = new HashMap<>();
        names.put("terms", terms);
        Map<String, Object> aggs = new HashMap<>();
        aggs.put(name, names);
        return aggs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSearch(AssetSearch search) {
        this.search = search;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Map<String, Object> getScriptParams() {
        return scriptParams;
    }

    public void setScriptParams(Map<String, Object> scriptParams) {
        this.scriptParams = scriptParams;
    }

    public String getExcludeRegex() {
        return excludeRegex;
    }

    public void setExcludeRegex(String excludeRegex) {
        this.excludeRegex = excludeRegex;
    }

    public String getIncludeRegex() {
        return includeRegex;
    }

    public void setIncludeRegex(String includeRegex) {
        this.includeRegex = includeRegex;
    }
}
