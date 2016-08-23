package com.zorroa.common.domain;

import java.util.Set;

/**
 * Created by chambers on 12/29/15.
 */
public class EventSearch {

    private Set<String> objectTypes;
    private Set<String> objectIds;
    private Set<String> tags;
    private String message;
    private String path;
    private long beforeTime = -1;
    private long afterTime = -1;

    private int limit = 50;
    private int page = 1;

    public EventSearch() { }

    public EventSearch(String message) {
        this.message = message;
    }

    public Set<String> getObjectTypes() {
        return objectTypes;
    }

    public EventSearch setObjectTypes(Set<String> objectTypes) {
        this.objectTypes = objectTypes;
        return this;
    }

    public Set<String> getObjectIds() {
        return objectIds;
    }

    public EventSearch setObjectIds(Set<String> objectIds) {
        this.objectIds = objectIds;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public EventSearch setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public EventSearch setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getPath() {
        return path;
    }

    public EventSearch setPath(String path) {
        this.path = path;
        return this;
    }

    public long getBeforeTime() {
        return beforeTime;
    }

    public EventSearch setBeforeTime(long beforeTime) {
        this.beforeTime = beforeTime;
        return this;
    }

    public long getAfterTime() {
        return afterTime;
    }

    public EventSearch setAfterTime(long afterTime) {
        this.afterTime = afterTime;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public EventSearch setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public int getPage() {
        return page;
    }

    public EventSearch setPage(int page) {
        this.page = page;
        return this;
    }
}
