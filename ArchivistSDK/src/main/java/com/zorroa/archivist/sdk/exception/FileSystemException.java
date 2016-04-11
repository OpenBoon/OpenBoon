package com.zorroa.archivist.sdk.exception;

/**
 * Created by chambers on 4/11/16.
 */
public class FileSystemException extends AnalystException {

    public FileSystemException() {
        super();
    }

    public FileSystemException(String message) {
        super(message);
    }

    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileSystemException(Throwable cause) {
        super(cause);
    }

}
