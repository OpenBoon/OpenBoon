package com.zorroa.analyst.cluster;

import com.zorroa.common.cluster.client.MasterServerClient;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.sdk.zps.MetaZpsExecutor;

/**
 * Created by chambers on 5/9/17.
 */
public class ClusterProcess {

    private TaskStartT task;
    private int exitStatus = -1;
    private MetaZpsExecutor zpsExecutor;
    private boolean killed = false;
    private MasterServerClient client;

    public ClusterProcess(TaskStartT task) {
        this.task = task;
        this.client = new MasterServerClient(task.getMasterHost());

        // Retry forever
        this.client.setMaxRetries(-1);
        // Timeout read/writes after 10 seconds.
        this.client.setSocketTimeout(10000);
        // Time connection after 2 seconds.
        this.client.setConnectTimeout(2000);
    }

    public TaskStartT getTask() {
        return task;
    }

    public ClusterProcess setTask(TaskStartT task) {
        this.task = task;
        return this;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public ClusterProcess setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
        return this;
    }

    public MetaZpsExecutor getZpsExecutor() {
        return zpsExecutor;
    }

    public ClusterProcess setZpsExecutor(MetaZpsExecutor zpsExecutor) {
        this.zpsExecutor = zpsExecutor;
        return this;
    }

    public boolean isKilled() {
        return killed;
    }

    public ClusterProcess setKilled(boolean killed) {
        this.killed = killed;
        return this;
    }

    public int getId() {
        return task.getId();
    }

    public MasterServerClient getClient() {
        return client;
    }

    public ClusterProcess setClient(MasterServerClient client) {
        this.client = client;
        return this;
    }
}
