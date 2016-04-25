package com.zorroa.archivist.sdk.domain;

import java.util.Set;

/**
 * Created by chambers on 12/29/15.
 */
public class EventLogSearch {

    private Set<String> types;
    private Set<String> ids;
    private Set<String> tags;
    private String message;
    private String path;
    private long beforeTime = -1;
    private long afterTime = -1;

    private int limit = 50;
    private int page = 1;

    public EventLogSearch() { }

    public EventLogSearch(String message) {
        this.message = message;
    }

    public Set<String> getTypes() {
        return types;
    }

    public EventLogSearch setTypes(Set<String> types) {
        this.types = types;
        return this;
    }

    public Set<String> getIds() {
        return ids;
    }

    public EventLogSearch setIds(Set<String> ids) {
        this.ids = ids;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public EventLogSearch setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public EventLogSearch setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getPath() {
        return path;
    }

    public EventLogSearch setPath(String path) {
        this.path = path;
        return this;
    }

    public long getBeforeTime() {
        return beforeTime;
    }

    public EventLogSearch setBeforeTime(long beforeTime) {
        this.beforeTime = beforeTime;
        return this;
    }

    public long getAfterTime() {
        return afterTime;
    }

    public EventLogSearch setAfterTime(long afterTime) {
        this.afterTime = afterTime;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public EventLogSearch setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public int getPage() {
        return page;
    }

    public EventLogSearch setPage(int page) {
        this.page = page;
        return this;
    }
}
