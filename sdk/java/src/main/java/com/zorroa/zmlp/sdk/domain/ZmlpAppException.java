package com.zorroa.zmlp.sdk.domain;

public class ZmlpAppException extends RuntimeException {

    public ZmlpAppException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZmlpAppException(String message) {
        super(message);
    }

}
