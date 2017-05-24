package com.zorroa.archivist.cluster;

/**
 * Created by chambers on 5/5/17.
 */

import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.service.AnalystService;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.archivist.service.JobExecutorService;
import com.zorroa.archivist.service.JobService;
import com.zorroa.common.cluster.thrift.*;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.util.Json;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class MasterRpcServiceImpl implements MasterRpcService, MasterServerService.Iface {

    private static final Logger logger = LoggerFactory.getLogger(MasterRpcServiceImpl.class);

    @Autowired
    JobExecutorService jobExecutorService;

    @Autowired
    JobService jobService;

    @Autowired
    AnalystService analystService;

    @Autowired
    TaskDao taskDao;

    @Autowired
    EventLogService eventLogService;

    @Value("${archivist.cluster.command.port}")
    int port;

    private TNonblockingServerSocket transport;
    private TServer server;
    private Executor thread = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void start() {
        try {
            transport = new TNonblockingServerSocket(port);
            TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(transport);
            args.maxReadBufferBytes = 1024 * 1024 * 10;
            args.processor(new MasterServerService.Processor<MasterServerService.Iface>(this))
                .workerThreads(4)
                .selectorThreads(1)
                .protocolFactory(new TCompactProtocol.Factory())
                .transportFactory(new TFramedTransport.Factory());
            server = new TThreadedSelectorServer(args);
            thread.execute(() -> server.serve());

        } catch (TTransportException e) {
            throw new RuntimeException("Unable to start thrift server " + e, e);
        }
    }

    @PreDestroy
    public void stop() {
        server.stop();
    }

    @Override
    public void ping(AnalystT node) throws TException {
        try {
            AnalystSpec spec = new AnalystSpec();
            spec.setId(node.getId());
            spec.setArch(node.getArch());
            spec.setOs(node.getOs());
            spec.setData(node.isData());
            spec.setUpdatedTime(System.currentTimeMillis());
            spec.setThreadCount(node.getThreadCount());
            spec.setState(AnalystState.UP);
            spec.setTaskIds(node.getTaskIds());
            spec.setLoadAvg(node.getLoadAvg());
            spec.setUrl(node.getUrl());
            spec.setQueueSize(node.getQueueSize());
            spec.setThreadsUsed(node.getThreadsUsed());
            spec.setMetrics(Json.deserialize(node.getMetrics(), Json.GENERIC_MAP));
            analystService.register(spec);
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error processing analyst ping, {}", node, e);
        }
    }

    @Override
    public void reportTaskStarted(int id) throws CusterExceptionT, TException {
        if (id < 1) {
            return;
        }
        try {
            Task t = taskDao.get(id);
            jobService.setTaskState(t, TaskState.Running, TaskState.Queued);
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task started: ", e);
        }
    }

    @Override
    public void reportTaskStopped(int id, TaskStopT result) throws CusterExceptionT, TException {
        if (id < 1) {
            return;
        }
        try {
            Task t = taskDao.get(id);
            jobService.setTaskCompleted(t, result.getExitStatus());
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task stopped: ", e);
        }
    }

    @Override
    public void reportTaskRejected(int id, String reason) throws CusterExceptionT, TException {
        if (id < 1) {
            return;
        }
        try {
            Task t = taskDao.get(id);
            jobService.setTaskState(t, TaskState.Waiting, TaskState.Queued);
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e);
        }
    }

    @Override
    public void reportTaskStats(int id, TaskStatsT stats) throws CusterExceptionT, TException {
        if (id < 1) {
            return;
        }
        try {
            Task t = taskDao.get(id);
            jobService.incrementJobStats(t.getJobId(), stats.getSuccessCount(),
                    stats.getErrorCount(), stats.getWarningCount());
            jobService.incrementTaskStats(t.getTaskId(), stats.getSuccessCount(),
                    stats.getErrorCount(), stats.getWarningCount());
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e);
        }
    }

    @Override
    public void reportTaskErrors(int id, List<TaskErrorT> errors) throws CusterExceptionT, TException {
        try {
            Task t = taskDao.get(id);
            eventLogService.logAsync(t, errors);
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e);
        }
    }

    @Override
    public List<TaskStartT> queuePendingTasks(String url, int count) throws CusterExceptionT, TException {
        try {
            return jobExecutorService.queueWaitingTasks(url, count).get();
        } catch (Exception e) {
            logger.warn("Unable to queue pending tasks,", e);
            return Lists.newArrayList();
        }
    }

    @Override
    public void expand(int id, ExpandT expand) throws CusterExceptionT, TException {
        try {
            Task t = taskDao.get(id);
            jobService.expand(t, expand);
        } catch (Exception e) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error expanding job, parent task {}", id, e);
        }
    }
}
