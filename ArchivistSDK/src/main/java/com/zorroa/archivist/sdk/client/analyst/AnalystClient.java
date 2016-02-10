package com.zorroa.archivist.sdk.client.analyst;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by chambers on 2/8/16.
 */
public class AnalystClient {

    private final CloseableHttpClient client;
    private final List<HttpHost> hosts;

    public AnalystClient(String host) {
        this(Lists.newArrayList(new HttpHost(host)));
    }

    public AnalystClient(List<HttpHost> hosts) {
        this.hosts = hosts;
        this.client = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .build();
    }

    /**
     * Analyze the given path and return an AssetBuilder complete with all gathered
     * metadata.
     *
     * @param analyzeRequest
     * @return
     * @throws IOException
     */
    public void analyze(AnalyzeRequest analyzeRequest) throws IOException {
        Http.post(client, hosts.get(0), "/api/v1/analyze", analyzeRequest);
    }
}
