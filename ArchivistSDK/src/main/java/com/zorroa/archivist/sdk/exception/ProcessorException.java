package com.zorroa.archivist.sdk.exception;

import com.zorroa.archivist.sdk.exception.ArchivistException;

/**
 * Created by chambers on 11/3/15.
 */
public class ProcessorException extends ArchivistException {

    public ProcessorException() {
        super();
    }

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessorException(Throwable cause) {
        super(cause);
    }
}
