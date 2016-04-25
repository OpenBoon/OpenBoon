package com.zorroa.archivist.sdk.client;

/**
 * Created by chambers on 2/25/16.
 */
public class ClientException extends RuntimeException {

    public ClientException() {}
    public ClientException(String message) {
        super(message);
    }
    public ClientException(String message, Exception ex) {
        super(message, ex);
    }
}
