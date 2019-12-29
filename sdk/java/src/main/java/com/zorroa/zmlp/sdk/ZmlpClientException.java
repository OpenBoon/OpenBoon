package com.zorroa.zmlp.sdk;

public class ZmlpClientException extends RuntimeException {
    public ZmlpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZmlpClientException(String message) {
        super(message);
    }
}
