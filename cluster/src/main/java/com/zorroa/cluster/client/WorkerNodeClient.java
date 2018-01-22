package com.zorroa.cluster.client;

import com.zorroa.cluster.thrift.TaskKillT;
import com.zorroa.cluster.thrift.TaskResultT;
import com.zorroa.cluster.thrift.TaskStartT;
import com.zorroa.cluster.thrift.WorkerNodeService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

/**
 * Created by chambers on 5/8/17.
 */
public class WorkerNodeClient extends AbtractThriftClient {

    private WorkerNodeService.Client service;

    public WorkerNodeClient(String host, int port) {
        super(host, port);
        this.setMaxRetries(2);
        this.setConnectTimeout(5000);
    }

    public WorkerNodeClient(String address) {
        super(address);
        this.setMaxRetries(2);
        this.setConnectTimeout(5000);
    }

    public int getDefaultPort() {
        return 8098;
    }
    public void reconnect() throws TException {
        if (!isConnected() || service == null) {
            TProtocol protocol = connect();
            service = new WorkerNodeService.Client(protocol);
        }
    }

    public TaskResultT executeTask(TaskStartT task)  {
        return new Reconnect<TaskResultT>() {
            @Override
            protected TaskResultT wrap() throws TException {
                return service.executeTask(task);
            }
        }.execute();
    }

    public void killTask(TaskKillT kill) {
        new Reconnect<Void>() {
            @Override
            protected Void wrap() throws TException {
                service.killTask(kill);
                return null;
            }
        }.execute();
    }

    public void killAll() {
        new Reconnect<Void>() {
            @Override
            protected Void wrap() throws TException {
                service.killAll();
                return null;
            }
        }.execute();
    }
}
