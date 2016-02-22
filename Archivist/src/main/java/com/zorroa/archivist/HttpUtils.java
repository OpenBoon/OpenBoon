package com.zorroa.archivist;

import com.zorroa.archivist.sdk.util.FileUtils;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
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

    public static String countResponse(CountResponse response) {
        return new StringBuilder(128)
                .append("{\"count\":")
                .append(response.getCount())
                .append(",\"_shards\":{\"total\":")
                .append(response.getTotalShards())
                .append(",\"successful\":")
                .append(response.getSuccessfulShards())
                .append(",\"failed\":")
                .append(response.getFailedShards())
                .append("}}")
                .toString();
    }
}
