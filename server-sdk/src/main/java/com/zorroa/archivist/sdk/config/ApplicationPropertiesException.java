package com.zorroa.archivist.sdk.config;

/**
 * Created by chambers on 2/12/16.
 */
public class ApplicationPropertiesException extends RuntimeException {

    public ApplicationPropertiesException() {
        super();
    }

    public ApplicationPropertiesException(String msg) {
        super(msg);
    }

    public ApplicationPropertiesException(String msg, Throwable t) {
        super(msg, t);
    }

    public ApplicationPropertiesException(Throwable t) {
        super(t);
    }
}
