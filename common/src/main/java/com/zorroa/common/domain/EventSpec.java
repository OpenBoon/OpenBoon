package com.zorroa.common.domain;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.slf4j.helpers.MessageFormatter;

import java.util.Set;

/**
 * Created by chambers on 12/28/15.
 */
public class EventSpec {

    private long timestamp = System.currentTimeMillis();

    /**
     * The message.
     */
    private String message;

    /**
     * Additional search tags.
     */
    private Set<String> tags;

    /**
     * The unique ID of the object.
     */
    private String objectId;

    /**
     * The type of object.
     */
    private String objectType = "system";

    private Throwable exception;

    public EventSpec() {}

    public static EventSpec log(String msg, Object ... args) {
        return new EventSpec().setMessage(
                MessageFormatter.arrayFormat(msg, args).getMessage());
    }

    public static EventSpec log(EventLoggable object, String msg, Object ... args) {
        return new EventSpec()
                .setMessage(MessageFormatter.arrayFormat(msg, args).getMessage())
                .setObjectId(String.valueOf(object.getEventLogId()))
                .setObjectType(object.getEventLogType());
    }

    public static EventSpec log(EventLoggable object, Throwable exception, String msg, Object ... args) {
        return new EventSpec()
                .setMessage(MessageFormatter.arrayFormat(msg, args).getMessage())
                .setObjectId(String.valueOf(object.getEventLogId()))
                .setObjectType(object.getEventLogType())
                .setException(exception);
    }

    public String getMessage() {
        return message;
    }

    public EventSpec setMessage(String message) {
        this.message = message;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public EventSpec setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    public EventSpec tag(String ... tag) {
        if (tags == null) {
            tags = Sets.newHashSet();
        }
        for (String t: tag) {
            tags.add(t);
        }
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public EventSpec setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Throwable getException() {
        return exception;
    }

    public EventSpec setException(Throwable exception) {
        this.exception = exception;
        return this;
    }

    public String getObjectId() {
        return objectId;
    }

    public EventSpec setObjectId(String objectId) {
        this.objectId = objectId;
        return this;
    }

    public String getObjectType() {
        return objectType;
    }

    public EventSpec setObjectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("message", message)
                .add("exception", exception)
                .add("objectType", objectType)
                .add("objectId", objectId)
                .add("tags", tags)
                .toString();
    }
}
