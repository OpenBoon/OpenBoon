package com.zorroa.analyst.cluster;

import com.zorroa.analyst.service.ProcessManagerNgService;
import com.zorroa.common.cluster.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by chambers on 5/8/17.
 */
@Component
public class WorkerRpcServiceImpl implements WorkerRpcService {

    @Autowired
    ProcessManagerNgService processManagerNgService;

    private TNonblockingServerSocket transport;
    private TServer server;
    private Executor thread = Executors.newSingleThreadExecutor();

    @Value("${cluster.server.port}")
    int port;

    @PostConstruct
    public void start() {
        try {
            transport = new TNonblockingServerSocket(port);
            TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(transport)
                    .processor(new
                            WorkerNodeService.Processor<WorkerNodeService.Iface>(this))
                    .workerThreads(4)
                    .selectorThreads(1)
                    .protocolFactory(new TCompactProtocol.Factory())
                    .transportFactory(new TFramedTransport.Factory());
            args.maxReadBufferBytes = 1024 * 1024 * 10;
            server = new TThreadedSelectorServer(args);
            thread.execute(() -> server.serve());

        } catch (TTransportException e) {
            throw new RuntimeException("Unable to start thrift server " + e, e);
        }
    }


    @Override
    public TaskResultT executeTask(TaskStartT task) throws CusterExceptionT, TException {
        return null;
    }

    @Override
    public void kill(TaskKillT kill) throws CusterExceptionT, TException {
        processManagerNgService.kill(kill);
    }

    @Override
    public void killAll() throws CusterExceptionT, TException {

    }
}
