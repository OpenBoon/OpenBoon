package com.zorroa.archivist;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableMap;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.sdk.util.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.zorroa.archivist.domain.NetworkEnvironment.ON_PREM;

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

    public static String getLocation() {
        Region region = Regions.getCurrentRegion();
        if (region == null) {
            return ON_PREM;
        }
        else {
            return region.getName();
        }
    }

    public static final String getHostname(ApplicationProperties properties) {

        String hostname = properties.getString("server.fqdn", null);
        if (hostname != null) {
            return hostname;
        }

        if (!getLocation().equals(ON_PREM)) {

            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            requestBuilder = requestBuilder.setConnectTimeout(1000);
            requestBuilder = requestBuilder.setConnectionRequestTimeout(1000);

            try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultRequestConfig(requestBuilder.build()).build()) {

                HttpResponse response = httpClient.execute(new HttpGet(
                        "http://169.254.169.254/latest/meta-data/public-ipv4"));
                HttpEntity entity = response.getEntity();
                hostname = EntityUtils.toString(entity, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException("AWS detected but unable to determine public interface", e);
            }
        }

        if (hostname == null) {
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (Exception ignore1) {
                try {
                    hostname = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception ignore2) {

                }
            }
            if (hostname == null) {
                throw new RuntimeException("Unable to determine public interface");
            }
        }

        return hostname;
    }

    public static final URI getUrl(ApplicationProperties properties) {
        StringBuilder url = new StringBuilder(256);
        url.append("http");
        if (properties.getBoolean("server.ssl.enabled")) {
            url.append("s");
        }
        url.append("://");
        url.append(properties.getString("server.address", getHostname(properties)));
        url.append(":");
        url.append(properties.getInt("server.port"));
        return URI.create(url.toString());
    }
}
