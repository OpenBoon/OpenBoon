package com.zorroa.zmlp.client.domain.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zorroa.zmlp.client.Json;
import org.apache.http.client.HttpResponseException;

import java.util.Map;
import java.util.Optional;

public class ZmlpRequestException extends ZmlpAppException {

    private String exception;

    private String path;

    private String status;

    public ZmlpRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZmlpRequestException(String stringData, String path, Exception cause) {
        super(cause.getMessage(), cause);

        Map data = null;
        try {
            data = Json.mapper.readValue(stringData, Map.class);
        } catch (JsonProcessingException e) {
            throw new ZmlpClientException("Failed on Json Parsing", e);
        }

        Optional.ofNullable(data.get("exception")).ifPresent(value -> exception = value.toString());
        Optional.ofNullable(data.get("status")).ifPresent(value -> status = value.toString());
        this.path = path;
    }

    public String getException() {
        return exception;
    }

    public ZmlpRequestException setException(String exception) {
        this.exception = exception;
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
