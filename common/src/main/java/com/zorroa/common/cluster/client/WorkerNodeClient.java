package com.zorroa.common.cluster.client;

import com.zorroa.common.cluster.thrift.TaskKillT;
import com.zorroa.common.cluster.thrift.TaskResultT;
import com.zorroa.common.cluster.thrift.TaskStartT;
import com.zorroa.common.cluster.thrift.WorkerNodeService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

/**
 * Created by chambers on 5/8/17.
 */
public class WorkerNodeClient extends AbtractThriftClient {

    private WorkerNodeService.Client service;

    public WorkerNodeClient(String host, int port) {
        super(host, port);
    }

    public WorkerNodeClient(String address) {
        super(address);
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
