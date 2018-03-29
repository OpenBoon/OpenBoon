package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Task
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
import java.util.*
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
            spec.taskIds = node.getTaskIds().map { UUID.fromString(it) }
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
    override fun reportTaskStarted(id: String) {
        if (id == null) {
            return
        }
        try {
            val t = getTask(id)
            jobService.setTaskRunning(t)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task started: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskStopped(id: String, result: TaskStopT) {
        if (id == null) {
            return
        }
        try {
            val t = getTask(id)
            jobService.setTaskCompleted(t, result.exitStatus, result.isKilled)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task stopped: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskRejected(id: String, reason: String) {
        if (id == null) {
            return
        }
        try {
            val t = getTask(id)
            jobService.setTaskState(t, TaskState.Waiting, TaskState.Queued)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskStats(id: String, stats: TaskStatsT) {
        if (id == null) {
            return
        }
        val adder = TaskStatsAdder(stats)
        try {
            val t = getTask(id)
            jobService.incrementStats(t, adder)
        } catch (e: Exception) {
            /**
             * Don't let this bubble out back to analyst.
             */
            logger.warn("Error reporting task rejected: ", e)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun reportTaskErrors(id: String, errors: List<TaskErrorT>) {
        try {
            val t = getTask(id)
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
        return try {
            jobExecutorService.queueWaitingTasks(url, count).get()
        } catch (e: Exception) {
            logger.warn("Unable to queue pending tasks,", e)
            Lists.newArrayList()
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun expand(id: String, expand: ExpandT) {
        try {
            val t = getTask(id)
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
            throw RuntimeException("Unable to start thrift server $e", e)
        }

    }

    private fun getTask(id: String) : Task {
        return taskDao.get(UUID.fromString(id))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MasterRpcServiceImpl::class.java)
    }
}
