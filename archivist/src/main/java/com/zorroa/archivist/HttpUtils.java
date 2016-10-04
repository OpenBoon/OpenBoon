package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Utility functions for any shared HTTP based code.
 */
public class HttpUtils {

    /**
     * Write an elastic XContent to the the given HttpServletResponse.
     *
     * @param result
     * @param response
     * @throws IOException
     */
    public static void writeElasticResponse(ToXContent result, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OutputStream out = response.getOutputStream();
        writeElasticResponse(result, out);
    }

    public static void writeElasticResponse(ToXContent result, OutputStream out) throws IOException {
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        try {
            content.startObject();
            result.toXContent(content, ToXContent.EMPTY_PARAMS);
            content.endObject();
        } finally {
            content.close();
            FileUtils.close(out);
        }
    }

    public static Map<String, Object> status(String type, Object id, String op, boolean success) {
        return ImmutableMap.of("type", type, "id", id, "op", op, "success", success);
    }

    public static Map<String, Object> count(Number count) {
        return ImmutableMap.of("count", count);
    }

    public static Map<String, Object> deleted(String type, Object id, boolean result) {
        return ImmutableMap.of("type", type, "id", id, "op", "delete", "success", result);
    }

    public static Map<String, Object> updated(String type, Object id, boolean success, Object object) {
        return ImmutableMap.of("type", type, "id", id, "op", "update", "success", success, "object", object);
    }

    public static String getBindingErrorString(BindingResult binding) {
        StringBuilder sb = new StringBuilder(1024);
        for (FieldError err: binding.getFieldErrors()) {
            sb.append("The '");
            sb.append(err.getField());
            sb.append("' field ");
            sb.append(err.getDefaultMessage());
            sb.append(", ");
        }
        sb.delete(sb.length()-2, sb.length()-1);
        return sb.toString();
    }
}
