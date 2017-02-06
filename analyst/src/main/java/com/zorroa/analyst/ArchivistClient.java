package com.zorroa.analyst;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.common.cluster.AbstractClient;
import com.zorroa.common.cluster.Http;
import com.zorroa.common.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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

    public Map<String, String> getClusterSettings() {
        return Http.get(client, loadBalancer.nextHost(), "/cluster/v1/settings",
                new TypeReference<Map<String, String>>() {});
    }

    /**
     * Register an analyst with the archivist.
     *
     * @param spec
     */
    public void register(AnalystSpec spec) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/analyst/_register",  spec);
    }


    /**
     * Send back a response
     *
     * @param response
     */
    public void respond(ExecuteTaskResponse response) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_response", response);
    }

    /**
     * Increment running task stats.
     *
     * @param stats
     */
    public void reportTaskStats(ExecuteTaskStats stats) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_stats", stats);
    }

    /**
     * Execute a reaction from a currently running zps script.
     *
     * @param spec
     */
    public void expand(ExecuteTaskExpand spec) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_expand", spec);
    }

    /**
     * Mark a task as running.
     *
     * @param task
     */
    public void reportTaskStarted(ExecuteTaskStarted task) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_running", task);
    }

    /**
     * Mark a task as completed.
     *
     * @param result
     */
    public void reportTaskStopped(ExecuteTaskStopped result) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_stopped", result);
    }

    /**
     * Obtain enough tasks to fill the queue.
     *
     * @param req
     */
    public List<ExecuteTaskStart> queueNextTasks(ExecuteTaskRequest req) {
        return Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_queue", req,
                new TypeReference<List<ExecuteTaskStart>>() {});
    }

    /**
     * Obtain enough tasks to fill the queue.
     *
     * @param req
     */
    public void rejectTask(ExecuteTaskStopped req) {
        Http.post(client, loadBalancer.nextHost(), "/cluster/v1/task/_reject", req);
    }
}
