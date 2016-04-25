package com.zorroa.archivist.sdk.exception;

/**
 * Created by chambers on 12/4/15.
 */
public class IngestException extends AnalystException {

    public IngestException() {
        super();
    }

    public IngestException(String message) {
        super(message);
    }

    public IngestException(String message, Throwable cause) {
        super(message, cause);
    }

    public IngestException(Throwable cause) {
        super(cause);
    }

}
