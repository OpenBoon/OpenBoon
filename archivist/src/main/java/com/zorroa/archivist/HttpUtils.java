package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.http.MediaType;

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

    public static Map<String, Object> status(String type, Object id, String op, boolean result) {
        return ImmutableMap.of("type", type, "id", id, "op", op, "status", result);
    }

    public static Map<String, Object> count(Number count) {
        return ImmutableMap.of("count", count);
    }

    public static Map<String, Object> deleted(String type, Object id, boolean result) {
        return ImmutableMap.of("type", type, "id", id, "op", "delete", "status", result);
    }

    public static Map<String, Object> updated(String type, Object id, boolean result, Object object) {
        return ImmutableMap.of("type", type, "id", id, "op", "update", "status", result, "object", object);
    }
}
