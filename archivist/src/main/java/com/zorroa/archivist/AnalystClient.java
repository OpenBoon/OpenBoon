package com.zorroa.archivist;

import com.zorroa.common.cluster.AbstractClient;
import com.zorroa.common.cluster.Http;
import com.zorroa.common.cluster.Protocol;
import com.zorroa.common.domain.ExecuteTaskStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.Collection;

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

    public AnalystClient(KeyStore trustStore, Collection<String> hosts) {
        this.loadBalancer.addHosts(hosts);
        this.client = Http.initClient(trustStore);
    }

    public AnalystClient(KeyStore trustStore, String... hosts) {
        for (String host : hosts) {
            this.loadBalancer.addHost(host);
        }
        this.client = Http.initClient(trustStore);
    }

    /**
     * Executes the given task;
     *
     * @param task
     */
    public void execute(ExecuteTaskStart task) {
        Http.post(client, loadBalancer.nextHost(), "/api/v1/task/_execute", task);
    }
}
