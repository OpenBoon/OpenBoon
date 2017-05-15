package com.zorroa.common.cluster.client;

/**
 * Created by chambers on 11/11/16.
 */
public class ClusterException extends RuntimeException {

    public ClusterException() {
        super();
    }

    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterException(Throwable cause) {
        super(cause);
    }
}
