package com.zorroa.archivist.web.exceptions;

import com.zorroa.archivist.ArchivistException;

/**
 * Created by chambers on 12/18/15.
 */
public class NullResultException extends ArchivistException {

    public NullResultException() {
        super();
    }

    public NullResultException(String message) {
        super(message);
    }

    public NullResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public NullResultException(Throwable cause) {
        super(cause);
    }
}
