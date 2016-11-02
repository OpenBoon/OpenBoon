package com.zorroa.common.cluster;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.sdk.client.ClientError;
import com.zorroa.sdk.client.ClientException;
import com.zorroa.sdk.util.Json;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;

/**
 * An convenience class for making HTTP/HTTPS requests with Apache HttpClient.
 */
public class Http {

    private static final Logger logger = LoggerFactory.getLogger(Http.class);

    public static <T> T post(HttpClient client, HttpHost host, String url, Object body, Class<T> resultType) {
        if (logger.isDebugEnabled()) {
            logger.debug("POST {} {}", url, body);
        }
        HttpPost post = new HttpPost(url);
        if (body != null) {
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new ByteArrayEntity(Json.serialize(body)));
        }
        HttpResponse response = checkStatus(client,host, post);
        return checkResponse(response, resultType);
    }

    public static <T> T post(HttpClient client, HttpHost host, String url, Object body, TypeReference<T> type) {
        if (logger.isDebugEnabled()) {
            logger.debug("POST {} {}", url, body);
        }
        HttpPost post = new HttpPost(url);
        if (body != null) {
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new ByteArrayEntity(Json.serialize(body)));
        }
        HttpResponse response = checkStatus(client,host, post);
        return checkResponse(response, type);
    }

    public static void post(HttpClient client, HttpHost host, String url, Object body) {
        if (logger.isDebugEnabled()) {
            logger.debug("POST {} {}", url, body);
        }
        HttpPost post = new HttpPost(url);
        if (body != null) {
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new ByteArrayEntity(Json.serialize(body)));
        }
        checkStatus(client, host, post);
    }

    public static <T> T get(HttpClient client, HttpHost host, String url, Class<T> resultType) {
        return checkResponse(checkStatus(client,host, new HttpGet(url)), resultType);
    }

    public static <T> T get(HttpClient client, HttpHost host, String url, TypeReference<T> type) {
        return checkResponse(checkStatus(client,host, new HttpGet(url)), type);
    }

    public static <T> T checkResponse(HttpResponse response, Class<T> resultType) {
        try {
            return Json.Mapper.readValue(response.getEntity().getContent(), resultType);
        } catch (Exception e) {
            throw new ClientException("Failed to deserialize response", e);
        }
    }

    public static <T> T checkResponse(HttpResponse response, TypeReference<T> type) {
        try {
            return Json.Mapper.readValue(response.getEntity().getContent(), type);
        }
        catch (Exception e) {
            throw new ClientException("Failed to deserialize response", e);
        }
    }

    public static HttpResponse checkStatus(HttpClient client, HttpHost host, HttpRequest req) {
        HttpResponse response;
        try {
            response = client.execute(host, req);
        } catch (Exception e) {
            /*
             * This would be some kind of communication error.
             */
            throw new ClientException("Failed to execute request: " + req, e);
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            ClientError error = checkResponse(response, ClientError.class);
            error.throwException();
        }

        return response;
    }

    public static CloseableHttpClient initClient() {
        SSLConnectionSocketFactory factory = null;

        try {
            SSLContext ctx = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build();
            factory = new SSLConnectionSocketFactory(ctx, new NoopHostnameVerifier());
        } catch (Exception e) {
            logger.warn("Failed to initialize SSL config, ", e);
        }

        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build();
        return HttpClients.custom()
                .setConnectionManagerShared(true)
                .setSSLSocketFactory(factory)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
