package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A general search object, represents an elasticsearch query.
 */
public class LogSearch {

    private Map<String, Map<String, Object>> query;

    private Map<String, Map<String, Object>> aggs;

    public static final Map<String, Map<String, Object>> DEFAULT_QUERY = ImmutableMap.of("match_all", ImmutableMap.of());

    public LogSearch() {
        query = DEFAULT_QUERY;
    }

    public Map<String, Map<String, Object>> getQuery() {
        return query;
    }

    public LogSearch setQuery(Map<String, Map<String, Object>> query) {
        this.query = query;
        return this;
    }

    public Map<String, Map<String, Object>> getAggs() {
        return aggs;
    }

    public LogSearch setAggs(Map<String, Map<String, Object>> aggs) {
        this.aggs = aggs;
        return this;
    }
}
