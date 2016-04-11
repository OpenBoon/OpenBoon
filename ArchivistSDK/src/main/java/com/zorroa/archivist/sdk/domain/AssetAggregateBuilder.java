/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.ImmutableMap;

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
    private AssetScript script;
    private String exclude;
    private String include;
    private boolean orderByTerm = true;

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
            terms.put("script", script.getScript());
            terms.put("lang", "native");
            terms.put("params", script.getParams());
        }
        if (exclude != null) {
            terms.put("exclude", exclude);
        }
        if (include != null) {
            terms.put("include", include);
        }

        if (orderByTerm == true) {
            terms.put("order", ImmutableMap.of("_term", "asc"));
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

    public AssetAggregateBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public AssetAggregateBuilder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public String getField() {
        return field;
    }

    public AssetAggregateBuilder setField(String field) {
        this.field = field;
        return this;
    }

    public int getSize() {
        return size;
    }

    public AssetAggregateBuilder setSize(int size) {
        this.size = size;
        return this;
    }

    public AssetScript getScript() {
        return script;
    }

    public AssetAggregateBuilder setScript(AssetScript script) {
        this.script = script;
        return this;
    }

    public String getExclude() {
        return exclude;
    }

    public AssetAggregateBuilder setExclude(String exclude) {
        this.exclude = exclude;
        return this;
    }

    public String getInclude() {
        return include;
    }

    public AssetAggregateBuilder setInclude(String include) {
        this.include = include;
        return this;
    }

    public boolean isOrderByTerm() {
        return orderByTerm;
    }

    public AssetAggregateBuilder setOrderByTerm(boolean orderByTerm) {
        this.orderByTerm = orderByTerm;
        return this;
    }
}
