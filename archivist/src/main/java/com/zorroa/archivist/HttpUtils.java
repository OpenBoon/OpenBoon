package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.zorroa.common.config.ApplicationProperties;
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
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    public static Map<String, Object> status(String type, String op, boolean success) {
        return ImmutableMap.of("type", type, "op", op, "success", success);
    }

    public static Map<String, Object> count(Number count) {
        return ImmutableMap.of("count", count);
    }

    public static Map<String, Object> counts(List<Long> counts) {
        return ImmutableMap.of("counts", counts);
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

    private static final char[] SYMBOLS;
    static {
        StringBuilder tmp = new StringBuilder();
        for (char ch = '0'; ch <= '9'; ++ch)
            tmp.append(ch);
        for (char ch = 'a'; ch <= 'z'; ++ch)
            tmp.append(ch);
        SYMBOLS = tmp.toString().toCharArray();
    }

    public static final String randomString(int length) {
        Random random = new Random();
        char[] buf = new char[length];
        for (int i=0; i<length; i++) {
            buf[i] = SYMBOLS[random.nextInt(SYMBOLS.length)];
        }
        return new String(buf);
    }

    public static final String getHostname() {
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignore1) {
            try {
                hostname = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignore2) {

            }
        }
        return hostname;
    }

    public static final String getUrl(ApplicationProperties properties) {
        StringBuilder url = new StringBuilder(256);
        url.append("http");
        if (properties.getBoolean("server.ssl.enabled")) {
            url.append("s");
        }
        url.append("://");
        url.append(properties.getString("server.address", getHostname()));
        url.append(":");
        url.append(properties.getInt("server.port"));
        return url.toString();
    }
}
