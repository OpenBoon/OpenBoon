package com.zorroa.common.domain;

import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 7/27/16.
 */
public class Event {

    private String id;
    private String message;
    private Set<String> tags;
    private String objectType;
    private String objectId;
    private String level;
    private List<String> stack;

    public String getId() {
        return id;
    }

    public Event setId(String id) {
        this.id = id;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Event setMessage(String message) {
        this.message = message;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Event setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getObjectType() {
        return objectType;
    }

    public Event setObjectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    public String getObjectId() {
        return objectId;
    }

    public Event setObjectId(String objectId) {
        this.objectId = objectId;
        return this;
    }

    public String getLevel() {
        return level;
    }

    public Event setLevel(String level) {
        this.level = level;
        return this;
    }

    public List<String> getStack() {
        return stack;
    }

    public Event setStack(List<String> stack) {
        this.stack = stack;
        return this;
    }
}
