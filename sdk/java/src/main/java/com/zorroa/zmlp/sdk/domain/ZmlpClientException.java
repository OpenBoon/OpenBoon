package com.zorroa.zmlp.sdk.domain;

public class ZmlpClientException extends ZmlpAppException {
    public ZmlpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZmlpClientException(String message) {
        super(message);
    }
}
