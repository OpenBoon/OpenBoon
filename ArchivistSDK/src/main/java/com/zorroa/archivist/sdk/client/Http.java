package com.zorroa.archivist.sdk.client;

import com.zorroa.archivist.sdk.util.Json;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
 * Created by chambers on 2/8/16.
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

    public static CloseableHttpClient initSSLClient(KeyStore keyStore, String keyPass, KeyStore trustStore) throws Exception {

        SSLContext ctx = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyPass.toCharArray())
                .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                .build();

        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(ctx, new NoopHostnameVerifier());

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("https", factory)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        return HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }
}
