package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.TaskStatsAdder
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.cluster.thrift.*
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.domain.TaskState
import com.zorroa.sdk.util.Json
import org.apache.thrift.TException
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.server.TServer
import org.apache.thrift.server.TThreadedSelectorServer
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TNonblockingServerSocket
import org.apache.thrift.transport.TTransportException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

interface MasterRpcService

@Component
class MasterRpcServiceImpl @Autowired constructor(
        private val jobExecutorService: JobExecutorService,
        private val jobService: JobService,
        private val analystService: AnalystService,
        private val taskDao: TaskDao,
        private val eventLogService: EventLogService
): MasterRpcService, MasterServerService.Iface, ApplicationListener<ContextRefreshedEvent> {

    @Value("\${archivist.cluster.command.port}")
    internal var port: Int = 0

    private var transport: TNonblockingServerSocket? = null
    private var server: TServer? = null
    private val thread = Executors.newSingleThreadExecutor()

    @PreDestroy
    fun stop() {
        server!!.stop()
    }

    @Throws(TException::class)
    override fun ping(node: AnalystT) {
        try {
            val spec = AnalystSpec()
            spec.id = node.getId()
            spec.arch = node.getArch()
            spec.os = node.getOs()
            spec.isData = node.isData
            spec.updatedTime = System.currentTimeMillis()
            spec.threadCount = node.getThreadCount()
            spec.state = AnalystState.UP
            spec.taskIds = node.getTaskIds()
            spec.loadAvg = node.getLoadAvg()
            spec.url = node.getUrl()
            spec.queueSize = node.getQueueSize()
            spec.threadsUsed = node.getThreadsUsed()
            spec.metrics = Json.deserialize(node.getMetrics(), Json.GENERIC_MAP)
            spec.version = node.getVersion()

            analystService.register(spec)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error processing analyst ping, {}", node, e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskStarted(id: Int) {
        if (id < 1) {
            return
        }
        try {
            val t = taskDao.get(id)
            jobService.setTaskRunning(t)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task started: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskStopped(id: Int, result: TaskStopT) {
        if (id < 1) {
            return
        }
        try {
            val t = taskDao.get(id)
            jobService.setTaskCompleted(t, result.getExitStatus())
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task stopped: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskRejected(id: Int, reason: String) {
        if (id < 1) {
            return
        }
        try {
            val t = taskDao.get(id)
            jobService.setTaskState(t, TaskState.Waiting, TaskState.Queued)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskStats(id: Int, stats: TaskStatsT) {
        if (id < 1) {
            return
        }
        val adder = TaskStatsAdder(stats)
        try {
            val t = taskDao.get(id)
            jobService.incrementStats(t, adder)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskErrors(id: Int, errors: List<TaskErrorT>) {
        try {
            val t = taskDao.get(id)
            eventLogService.logAsync(t, errors)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun queuePendingTasks(url: String, count: Int): List<TaskStartT> {
        try {
            return jobExecutorService.queueWaitingTasks(url, count).get()
        } catch (e: Exception) {
            logger.warn("Unable to queue pending tasks,", e)
            return Lists.newArrayList()
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun expand(id: Int, expand: ExpandT) {
        try {
            val t = taskDao.get(id)
            jobService.expand(t, expand)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error expanding job, parent task {}", id, e)
        }

    }

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        try {
            transport = TNonblockingServerSocket(port)
            val args = TThreadedSelectorServer.Args(transport)
            args.maxReadBufferBytes = (1024 * 1024 * 10).toLong()
            args.processor(MasterServerService.Processor<MasterServerService.Iface>(this))
                    .workerThreads(4)
                    .selectorThreads(1)
                    .protocolFactory(TCompactProtocol.Factory())
                    .transportFactory(TFramedTransport.Factory())
            server = TThreadedSelectorServer(args)
            thread.execute { server!!.serve() }

        } catch (e: TTransportException) {
            throw RuntimeException("Unable to start thrift server " + e, e)
        }

    }

    companion object {

        private val logger = LoggerFactory.getLogger(MasterRpcServiceImpl::class.java)
    }
}
