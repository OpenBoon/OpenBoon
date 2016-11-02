package com.zorroa.archivist;

import com.zorroa.common.cluster.AbstractClient;
import com.zorroa.common.cluster.Http;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.ExecuteTaskStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /**
     * Executes the given task;
     *
     * @param task
     */
    public void execute(ExecuteTaskStart task) {
        Http.post(client, loadBalancer.nextHost(), "/api/v1/task/_execute", task);
    }

    /**
     * Executes the given task;
     *
     * @param task
     */
    public void stop(ExecuteTaskStop task) {
        Http.post(client, loadBalancer.nextHost(), "/api/v1/task/_stop", task);
    }
}
