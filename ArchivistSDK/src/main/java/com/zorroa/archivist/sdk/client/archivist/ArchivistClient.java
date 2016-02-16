package com.zorroa.archivist.sdk.client.archivist;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        this.client = Http.initSSLClient(keyStore, keyPass, trustStore);
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
