package com.zorroa.archivist.sdk.exception;

/**
 * Created by chambers on 12/2/15.
 */
public class InvalidateStateException extends ArchivistException {

    public InvalidateStateException() {
        super();
    }

    public InvalidateStateException(String message) {
        super(message);
    }

    public InvalidateStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidateStateException(Throwable cause) {
        super(cause);
    }
}
