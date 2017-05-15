package com.zorroa.common.cluster.client;

import com.zorroa.common.cluster.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

import java.net.URI;
import java.util.List;

/**
 * Created by chambers on 5/8/17.
 */
public class MasterServerClient extends AbtractThriftClient {

    private MasterServerService.Client service = null;

    public MasterServerClient(String address) {
        super(address);
    }

    public MasterServerClient(URI uri) {
        super(uri.getHost(), uri.getPort());
    }

    public MasterServerClient(String host, int port) {
        super(host, port);
    }

    public int getDefaultPort() {
        return 8065;
    }

    public void reconnect() throws TException {
        if (!isConnected() || service == null) {
            TProtocol protocol = connect();
            service = new MasterServerService.Client(protocol);
        }
    }

    public void ping(AnalystT node) {
        new Reconnect<Void>(true) {
            @Override
            protected Void wrap() throws TException {
                service.ping(node);
                return null;
            }
        }.execute();
    }

    public void reportTaskStarted(int id) {
        new Reconnect<Void>(true) {
            @Override
            protected Void wrap() throws TException {
                service.reportTaskStarted(id);
                return null;
            }
        }.execute();
    }

    public void reportTaskStopped(int id, TaskStopT result) {
        new Reconnect<Void>(true) {
            @Override
            protected Void wrap() throws TException {
                service.reportTaskStopped(id, result);
                return null;
            }
        }.execute();
    }

    public void reportTaskResult(int id, TaskResultT result) {
        new Reconnect<Void>(true) {
            @Override
            protected Void wrap() throws TException {
                service.reportTaskResult(id, result);
                return null;
            }
        }.execute();
    }

    public void reportTaskRejected(int id, String reason) {
        new Reconnect<Void>(true) {
            @Override
            protected Void wrap() throws TException {
                service.reportTaskRejected(id, reason);
                return null;
            }
        }.execute();
    }

    public void reportTaskStats(int id, TaskStatsT stats) {
        new Reconnect<Void>(false) {
            @Override
            protected Void wrap() throws TException {
                service.reportTaskStats(id, stats);
                return null;
            }
        }.execute();
    }

    public List<TaskStartT> queuePendingTasks(String url, int count) {
        return new Reconnect<List<TaskStartT>>(true) {
            @Override
            protected List<TaskStartT> wrap() throws TException {
                return service.queuePendingTasks(url,  count);
            }
        }.execute();
    }

    public void expand(int parent, ExpandT expand) {
        new Reconnect<Void>(true) {
            @Override
            protected Void wrap() throws TException {
                service.expand(parent, expand);
                return null;
            }
        }.execute();
    }

}
