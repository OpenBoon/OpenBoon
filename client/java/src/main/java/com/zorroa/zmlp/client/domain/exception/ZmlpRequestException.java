package com.zorroa.zmlp.client.domain.exception;

import java.util.Map;
import java.util.Optional;

public class ZmlpRequestException extends ZmlpAppException{

    private String exception;

    private String cause;

    private String path;

    private String status;

    public ZmlpRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZmlpRequestException(Map<String,String> data, String message, Throwable cause) {
        super(message, cause);

        Optional.ofNullable(data.get("exception")).ifPresent(value->exception = value);

    }

    public String getException() {
        return exception;
    }

    public ZmlpRequestException setException(String exception) {
        this.exception = exception;
        return this;
    }

    public ZmlpRequestException setCause(String cause) {
        this.cause = cause;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ZmlpRequestException setPath(String path) {
        this.path = path;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ZmlpRequestException setStatus(String status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return String.format("<ZmlpRequestException msg=%s>", this.getMessage());
    }
}
