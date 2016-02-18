package com.zorroa.archivist.sdk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.sdk.util.Json;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;

/**
 * An convience class for making HTTP/HTTPS requests with Apache HttpClient.
 */
public class Http {

    public static <T> T post(HttpClient client, HttpHost host, String url, Object body, Class<T> resultType) {
        try {
            HttpPost post = new HttpPost(url);
            if (body != null) {
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new ByteArrayEntity(Json.serialize(body)));
            }
            HttpResponse response = client.execute(host, post);
            return Json.Mapper.readValue(response.getEntity().getContent(), resultType);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }

        return null;
    }

    public static <T> T post(HttpClient client, HttpHost host, String url, Object body, TypeReference<T> type) {
        try {
            HttpPost post = new HttpPost(url);
            if (body != null) {
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new ByteArrayEntity(Json.serialize(body)));
            }
            HttpResponse response = client.execute(host, post);
            return Json.Mapper.readValue(response.getEntity().getContent(), type);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }

        return null;
    }

    public static void post(HttpClient client, HttpHost host, String url, Object body) {
        try {
            HttpPost post = new HttpPost(url);
            if (body != null) {
                post.setHeader("Content-Type", "application/json");
                post.setEntity(new ByteArrayEntity(Json.serialize(body)));
            }
            HttpResponse response = client.execute(host, post);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }
    }

    public static <T> T get(HttpClient client, HttpHost host, String url, Class<T> resultType) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(host, get);
            return Json.Mapper.readValue(response.getEntity().getContent(), resultType);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }

        return null;
    }

    public static <T> T get(HttpClient client, HttpHost host, String url, TypeReference<T> type) {
        try {
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(host, get);
            return Json.Mapper.readValue(response.getEntity().getContent(), type);
        } catch (Exception e) {
            ExceptionTranslator.translate(e);
        }

        return null;
    }

    /**
     *
     * @param trustStore
     * @return
     * @throws Exception
     */
    public static CloseableHttpClient initClient(KeyStore trustStore) {
        PoolingHttpClientConnectionManager cm;
        try {
            SSLContext ctx = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                    .build();
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(ctx, new NoopHostnameVerifier());
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                    .<ConnectionSocketFactory> create().register("https", factory)
                    .build();
            cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        } catch (Exception e) {
            cm = new PoolingHttpClientConnectionManager();
        }

        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }
}
