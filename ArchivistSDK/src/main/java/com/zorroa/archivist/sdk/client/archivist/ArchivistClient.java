package com.zorroa.archivist.sdk.client.archivist;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.util.List;

/**
 * Created by chambers on 2/8/16.
 */
public class ArchivistClient {

    public static final int DEFAULT_PORT = 8066;

    private static final Logger logger = LoggerFactory.getLogger(ArchivistClient.class);

    private CloseableHttpClient client;
    private final List<HttpHost> hosts;

    public ArchivistClient(String host) {
        hosts = Lists.newArrayList();
        if (host.contains(":")) {
            String[] parts = host.split(":");
            hosts.add(new HttpHost(parts[0], Integer.valueOf(parts[1]), "https"));
        }
        else {
            hosts.add(new HttpHost(host, DEFAULT_PORT, "https"));
        }
    }


    public void init(KeyStore keyStore, String keyPass, KeyStore trustStore) throws Exception {

        SSLContext ctx = SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyPass.toCharArray())
                .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                .build();

        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(ctx, new NoopHostnameVerifier());

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("https", factory)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);

        this.client = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
    }

    /**
     * Register the given host/port as a worker node.
     */
    public void registerAnalyst(AnalystPing ping) {
        Http.post(client, hosts.get(0), "/api/v1/analyst/_register", ping);
    }

    /**
     * Register the given host/port as a worker node.
     */
    public void shutdownAnalyst(AnalystPing ping) {
        Http.post(client, hosts.get(0), "/api/v1/analyst/_shutdown", ping);
    }
}
