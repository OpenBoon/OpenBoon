package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * A general search object, represents an elasticsearch query.
 */
public class EventLogSearch {

    private Map<String, Map<String, Object>> query;

    private Map<String, Map<String, Object>> aggs = Maps.newHashMap();

    public static final Map<String, Map<String, Object>> DEFAULT_QUERY = ImmutableMap.of("match_all", ImmutableMap.of());

    public EventLogSearch() {
        query = DEFAULT_QUERY;
    }

    public Map<String, Map<String, Object>> getQuery() {
        return query;
    }

    public EventLogSearch setQuery(Map<String, Map<String, Object>> query) {
        this.query = query;
        return this;
    }

    public Map<String, Map<String, Object>> getAggs() {
        return aggs;
    }

    public EventLogSearch setAggs(Map<String, Map<String, Object>> aggs) {
        this.aggs = aggs;
        return this;
    }
}
