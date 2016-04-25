package com.zorroa.archivist.sdk.client.analyst;

import com.zorroa.archivist.sdk.client.AbstractClient;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.client.Protocol;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 2/8/16.
 */
public class AnalystClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(AnalystClient.class);

    public AnalystClient() {
        super();
    }

    public AnalystClient(String host) {
        super();
        this.getLoadBalancer().addHost(host);
    }

    public AnalystClient(KeyStore trustStore) {
        super(trustStore);
    }

    public AnalystClient(KeyStore trustStore, String host, int port, Protocol protocol) {
        super(trustStore);
        this.loadBalancer.addHost(host, port, protocol);
    }

    public AnalystClient(KeyStore trustStore,  Collection<String> hosts) {
        this.loadBalancer.addHosts(hosts);
        this.client = Http.initClient(trustStore);
    }

    public AnalystClient(KeyStore trustStore,  String ... hosts) {
        for (String host: hosts) {
            this.loadBalancer.addHost(host);
        }
        this.client = Http.initClient(trustStore);
    }

    /**
     * Analyze the given path and return an AssetBuilder complete with all gathered
     * metadata.
     *
     * @param analyzeRequest
     * @return
     * @throws IOException
     */
    public AnalyzeResult analyze(AnalyzeRequest analyzeRequest) {
       return Http.post(client, loadBalancer.nextHost(), "/api/v1/analyze", analyzeRequest, AnalyzeResult.class);
    }

    /**
     * Analyze the given path and return an AssetBuilder complete with all gathered
     * metadata.
     *
     * @return
     * @throws IOException
     */
    public List<String> getAvailableIngestors() {
        return Http.get(client, loadBalancer.nextHost(), "/api/v1/plugins/ingest", Json.LIST_OF_STRINGS);
    }
}
