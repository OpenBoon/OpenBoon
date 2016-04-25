package com.zorroa.archivist.sdk.exception;

/**
 * Created by chambers on 2/25/16.
 */
public class AnalystException extends RuntimeException {

    public AnalystException() {
        super();
    }

    public AnalystException(String message) {
        super(message);
    }

    public AnalystException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalystException(Throwable cause) {
        super(cause);
    }
}
