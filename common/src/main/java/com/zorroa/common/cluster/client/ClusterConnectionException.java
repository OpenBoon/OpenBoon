package com.zorroa.common.cluster.client;

/**
 * Created by chambers on 11/11/16.
 */
public class ClusterConnectionException extends ClusterException {

    public ClusterConnectionException() {
        super();
    }

    public ClusterConnectionException(String message) {
        super(message);
    }

    public ClusterConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterConnectionException(Throwable cause) {
        super(cause);
    }
}
