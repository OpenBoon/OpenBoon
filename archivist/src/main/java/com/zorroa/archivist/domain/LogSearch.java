package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * A general search object, represents an elasticsearch query.
 */
public class LogSearch {

    private Map<String, Object> query;

    public LogSearch() {
        query = ImmutableMap.of("match_all", ImmutableMap.of());
    }

    public Map<String, Object> getQuery() {
        return query;
    }

    public LogSearch setQuery(Map<String, Object> query) {
        this.query = query;
        return this;
    }


}
