package com.zorroa.archivist.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Created by chambers on 7/9/16.
 */
public class InvalidObjectException extends RuntimeException {

    private static final Logger log = LoggerFactory.getLogger(InvalidObjectException.class);
    private static final long serialVersionUID = -7882202987868263849L;

    private final BindingResult bindingResult;

    public InvalidObjectException(
            final String message,
            final BindingResult bindingResult) {
        super(getErrorMesage(message, bindingResult));
        this.bindingResult = bindingResult;
        log.error(getLocalizedMessage());
    }
    public BindingResult getBindingResult() {
        return bindingResult;
    }

    public static String getErrorMesage(String message, BindingResult br) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("Invalid object failure, ");
        for (FieldError error: br.getFieldErrors()) {
            sb.append("field: ")
            .append("[").append(error.getField()).append("] ");
            sb.append(error.getDefaultMessage());
            sb.append(", was: [").append(error.getRejectedValue()).append("]. ");
        }
        sb.append(message);
        return sb.toString();
    }
}
