package boonai.archivist.util;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Utility functions for any shared HTTP based code.
 */
public class HttpUtils {

    /**
     * A standard cache-control header.
     */
    public static final CacheControl CACHE_CONTROL =
            CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate();

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
            try {
                out.close();
            } catch (IOException e) {
                //ignore
            }
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

    public static Map<String, Object> exists(Object id, boolean value) {
        return ImmutableMap.of("exists", value, "id", id);
    }

    public static Map<String, Object> deleted(String type, Object id, boolean result) {
        return ImmutableMap.of("type", type, "id", id, "op", "delete", "success", result);
    }

    public static Map<String, Object> updated(String type, Object id, boolean success, Object object) {
        return ImmutableMap.of("type", type, "id", id, "op", "update", "success", success, "object", object);
    }

    public static Map<String, Object> updated(String type, Object id, boolean success) {
        return ImmutableMap.of("type", type, "id", id, "op", "update", "success", success);
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
}
