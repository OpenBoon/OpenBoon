package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 10/27/15.
 */
public class ArchivistException extends RuntimeException {

    public ArchivistException() {
        super();
    }

    public ArchivistException(String message) {
        super(message);
    }

    public ArchivistException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArchivistException(Throwable cause) {
        super(cause);
    }
}
