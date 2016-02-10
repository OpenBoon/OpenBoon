package com.zorroa.archivist.sdk.client.archivist;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.util.List;

/**
 * Created by chambers on 2/8/16.
 */
public class ArchivistClient {

    private final CloseableHttpClient client;
    private final List<HttpHost> hosts;

    public ArchivistClient(String host) {
        this(Lists.newArrayList(new HttpHost(host)));
    }

    public ArchivistClient(List<HttpHost> hosts) {
        this.hosts = hosts;
        this.client = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
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
        Http.post(client, hosts.get(0), "/api/v1/_shutdown", ping);
    }
}
