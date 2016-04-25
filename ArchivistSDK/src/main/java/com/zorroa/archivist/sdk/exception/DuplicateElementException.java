package com.zorroa.archivist.sdk.exception;

/**
 * Created by chambers on 10/27/15.
 */
public class DuplicateElementException extends ArchivistException {

    public DuplicateElementException() {
        super();
    }

    public DuplicateElementException(String message) {
        super(message);
    }

    public DuplicateElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateElementException(Throwable cause) {
        super(cause);
    }
}
