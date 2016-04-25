package com.zorroa.archivist.sdk.client.archivist;

import com.zorroa.archivist.sdk.client.AbstractClient;
import com.zorroa.archivist.sdk.client.Http;
import com.zorroa.archivist.sdk.client.Protocol;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.Collection;

/**
 * Created by chambers on 2/8/16.
 */
public class ArchivistClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistClient.class);

    public ArchivistClient() {
        super();
    }

    public ArchivistClient(String host) {
        super();
        this.getLoadBalancer().addHost(host);
    }

    public ArchivistClient(KeyStore trustStore) {
        super(trustStore);
    }

    public ArchivistClient(KeyStore trustStore, String host, int port, Protocol protocol) {
        super(trustStore);
        this.loadBalancer.addHost(host, port, protocol);
    }

    public ArchivistClient(KeyStore trustStore, Collection<String> hosts) {
        super(trustStore);
        this.loadBalancer.addHosts(hosts);
    }

    public ArchivistClient(KeyStore trustStore, String ... hosts) {
        super(trustStore);
        for (String host: hosts) {
            this.loadBalancer.addHost(host);
        }
    }
    /**
     * Register the given host/port as a worker node.
     */
    public void registerAnalyst(AnalystPing ping) {
        Http.post(client, loadBalancer.nextHost(), "/api/v1/analyst/_register", ping);
    }

    /**
     * Register the given host/port as a worker node.
     */
    public void shutdownAnalyst(AnalystPing ping) {
        Http.post(client, loadBalancer.nextHost(), "/api/v1/analyst/_shutdown", ping);
    }
}
