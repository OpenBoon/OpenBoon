package com.zorroa.archivist;

import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

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
        XContentBuilder content = XContentFactory.jsonBuilder(out);
        try {
            content.startObject();
            result.toXContent(content, ToXContent.EMPTY_PARAMS);
            content.endObject();
        } finally {
            content.close();
            close(out);
        }
    }

    /**
     * Handles the case where closing a stream may fail.
     *
     * @param c
     */
    private static final void close(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        }
        catch (IOException e) {
            // ignore
        }
    }
}
