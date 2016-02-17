package com.zorroa.archivist.sdk.client.analyst;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.util.Json;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chambers on 2/8/16.
 */
public class AnalystClient {

    public static final int DEFAULT_PORT = 8099;
    private CloseableHttpClient client;
    private final List<HttpHost> hosts;
    private final AtomicInteger requestCount  = new AtomicInteger();

    public AnalystClient(Collection<String> analysts) {
        hosts = Lists.newArrayList();
        for (String analsyst: analysts) {
            if (analsyst.contains(":")) {
                String[] parts = analsyst.split(":");
                hosts.add(new HttpHost(parts[0], Integer.valueOf(parts[1]), "https"));
            } else {
                hosts.add(new HttpHost(analsyst, DEFAULT_PORT, "https"));
            }
        }

        if (hosts.isEmpty()) {
            throw new IllegalArgumentException("Cannot initialize a client with no hosts");
        }
    }

    private HttpHost nextHost() {
        if (hosts.size() == 1) {
            return hosts.get(0);
        } else {
            /* Round robbin */
            int count = requestCount.incrementAndGet();
            return hosts.get(count % hosts.size());
        }
    }

    public void init(KeyStore keyStore, String keyPass, KeyStore trustStore) throws Exception {
        this.client = Http.initSSLClient(keyStore, keyPass, trustStore);
    }

    /**
     * Analyze the given path and return an AssetBuilder complete with all gathered
     * metadata.
     *
     * @param analyzeRequest
     * @return
     * @throws IOException
     */
    public AnalyzeResult analyze(AnalyzeRequest analyzeRequest) throws IOException {
       return Http.post(client, nextHost(), "/api/v1/analyze", analyzeRequest, AnalyzeResult.class);
    }

    /**
     * Analyze the given path and return an AssetBuilder complete with all gathered
     * metadata.
     *
     * @return
     * @throws IOException
     */
    public List<String> getAvailableIngestors() {
        return Http.get(client, nextHost(), "/api/v1/plugins/ingest", Json.LIST_OF_STRINGS);
    }
}
