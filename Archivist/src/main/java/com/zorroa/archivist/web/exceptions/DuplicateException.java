package com.zorroa.archivist.web.exceptions;

import com.zorroa.archivist.sdk.exception.ArchivistException;

/**
 * Created by chambers on 12/18/15.
 */
public class DuplicateException extends ArchivistException {

    public DuplicateException() {
        super();
    }

    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateException(Throwable cause) {
        super(cause);
    }
}
