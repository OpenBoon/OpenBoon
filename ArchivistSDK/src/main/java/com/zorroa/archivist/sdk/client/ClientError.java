package com.zorroa.archivist.sdk.client;

import com.google.common.base.MoreObjects;

/**
 * Created by chambers on 2/24/16.
 */
public class ClientError {

    private long timestamp;
    private String exception;
    private String message;

    public ClientError() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ClientError setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getException() {
        return exception;
    }

    public ClientError setException(String exception) {
        this.exception = exception;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ClientError setMessage(String message) {
        this.message = message;
        return this;
    }

    public void throwException() throws RuntimeException {
        RuntimeException e;
        try {
            Class c = Class.forName(exception);
            e = (RuntimeException) c.getConstructor(String.class).newInstance(message);

        } catch (Exception ex) {
            /*
             * A lot of things can go wrong with class loading
             * so just throw a IllegalArgument exception?
             */
            throw new IllegalArgumentException(message);
        }

        throw e;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", timestamp)
                .add("exception", exception)
                .add("message", message)
                .toString();
    }

}


