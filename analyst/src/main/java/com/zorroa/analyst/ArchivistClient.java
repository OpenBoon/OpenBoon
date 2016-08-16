package com.zorroa.analyst;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.zorroa.common.cluster.AbstractClient;
import com.zorroa.common.cluster.Http;
import com.zorroa.common.cluster.Protocol;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyStore;
import java.util.Collection;
import java.util.Map;

/**
 * Created by chambers on 2/8/16.
 */
public class ArchivistClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistClient.class);

    public ArchivistClient() {}

    public ArchivistClient(String host) {
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

    public ArchivistClient(KeyStore trustStore, String... hosts) {
        super(trustStore);
        for (String host: hosts) {
            this.loadBalancer.addHost(host);
        }
    }

    public Map<String, String> getClusterSettings() {
        return Http.get(client, loadBalancer.nextHost(), "/cluster/v1/settings",
                new TypeReference<Map<String, String>>() {});
    }

    /**
     * Execute a reaction from a currently running zps script.
     *
     * @param expand
     */
    public void expand(ZpsScript expand) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_expand", expand);
    }

    /**
     * Mark a task as running.
     *
     * @param script
     */
    public void reportTaskRunning(ZpsTask script) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_running",
                ImmutableMap.of(
                        "taskId", script.getTaskId(),
                        "jobId", script.getJobId()));
    }

    /**
     * Mark a task as completed.
     *
     * @param script
     */
    public void reportTaskCompleted(ZpsTask script, int exitStatus) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_completed",
                ImmutableMap.of(
                    "taskId", script.getTaskId(),
                    "jobId", script.getJobId(),
                    "exitStatus" , exitStatus));

    }
}
