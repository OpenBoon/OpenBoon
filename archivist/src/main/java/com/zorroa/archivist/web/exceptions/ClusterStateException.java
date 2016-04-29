package com.zorroa.archivist.web.exceptions;

import com.zorroa.sdk.exception.ArchivistException;

/**
 * Created by chambers on 3/22/16.
 */
public class ClusterStateException extends ArchivistException {

    public ClusterStateException() {
        super();
    }

    public ClusterStateException(String message) {
        super(message);
    }

    public ClusterStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterStateException(Throwable cause) {
        super(cause);
    }

}
