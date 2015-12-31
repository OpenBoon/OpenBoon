package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;
import org.slf4j.helpers.MessageFormatter;

import java.util.Set;

/**
 * Created by chambers on 12/28/15.
 */
public class EventLogMessage {

    private long timestamp = System.currentTimeMillis();
    private String message;
    private Set<String> tags;

    /**
     * The unique ID of the object.
     */
    private String id;

    /**
     * The type of object.
     */
    private String type;

    /**
     * The path to the asset (optional)
     */
    private String path;

    private Throwable exception;

    public EventLogMessage() {}

    public EventLogMessage(String message, Object ... args) {
        this.message = message;
        this.message =  MessageFormatter.arrayFormat(message, args).getMessage();
    }

    public EventLogMessage(Id object, String message, Object ... args) {
        this.id = String.valueOf(object.getId());
        this.type = object.getClass().getSimpleName();
        this.message =  MessageFormatter.arrayFormat(message, args).getMessage();
    }

    public EventLogMessage(Id object, String message, Throwable exception, Object ... args) {
        this.id = String.valueOf(object.getId());
        this.type = object.getClass().getSimpleName();
        this.message = MessageFormatter.arrayFormat(message, args).getMessage();
        this.exception = exception;
    }

    public EventLogMessage(Asset asset, String message, Object ... args) {
        this.id = asset.getId();
        this.type = "Asset";
        this.message =  MessageFormatter.arrayFormat(message, args).getMessage();
        this.path = asset.getValue("source.path");
    }

    public EventLogMessage(Asset asset, String message, Throwable exception, Object ... args) {
        this.id = asset.getId();
        this.type = "Asset";
        this.message = MessageFormatter.arrayFormat(message, args).getMessage();
        this.exception = exception;
        this.path = asset.getValue("source.path");
    }

    public String getMessage() {
        return message;
    }

    public EventLogMessage setMessage(String message) {
        this.message = message;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public EventLogMessage setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public String getPath() {
        return path;
    }

    public EventLogMessage setPath(String path) {
        this.path = path;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventLogMessage setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Throwable getException() {
        return exception;
    }

    public EventLogMessage setException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public String getId() {
        return id;
    }

    public EventLogMessage setId(String id) {
        this.id = id;
        return this;
    }

    public String getType() {
        return type;
    }

    public EventLogMessage setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("message", message)
                .add("exception", exception)
                .add("object", type)
                .add("id", id)
                .add("tags", tags)
                .add("path", path)
                .toString();
    }

}
