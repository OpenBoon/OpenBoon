package com.zorroa.analyst.cluster

import com.zorroa.analyst.service.ProcessManagerService
import com.zorroa.cluster.client.MasterServerClient
import com.zorroa.cluster.thrift.*
import com.zorroa.cluster.zps.MetaZpsExecutor
import org.apache.thrift.TException
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.server.TServer
import org.apache.thrift.server.TThreadedSelectorServer
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TNonblockingServerSocket
import org.apache.thrift.transport.TTransportException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

interface WorkerRpcService : WorkerNodeService.Iface

data class ClusterProcess constructor (val task: TaskStartT) {
    var exitStatus = -1
    var zpsExecutor: MetaZpsExecutor? = null
    var killed = false
    val client: MasterServerClient = MasterServerClient(task.getMasterHost())

    val id: Int
        get() = task.getId()

    init {
        // Retry forever
        this.client.maxRetries = -1
        // Timeout read/writes after 10 seconds.
        this.client.socketTimeout = 10000
        // Time connection after 2 seconds.
        this.client.connectTimeout = 2000
    }

    fun setExitStatus(exitStatus: Int): ClusterProcess {
        this.exitStatus = exitStatus
        return this
    }

    fun setZpsExecutor(zpsExecutor: MetaZpsExecutor): ClusterProcess {
        this.zpsExecutor = zpsExecutor
        return this
    }

    fun setKilled(killed: Boolean): ClusterProcess {
        this.killed = killed
        return this
    }
}


@Component
class WorkerRpcServiceImpl @Autowired constructor (
        private val processManagerService: ProcessManagerService
) : WorkerRpcService {

    private var transport: TNonblockingServerSocket? = null
    private var server: TServer? = null
    private val thread = Executors.newSingleThreadExecutor()

    @Value("\${analyst.cluster.command.port}")
    internal var port: Int = 0

    @PostConstruct
    fun start() {
        try {
            transport = TNonblockingServerSocket(port)
            val args = TThreadedSelectorServer.Args(transport)
                    .processor(WorkerNodeService.Processor<WorkerNodeService.Iface>(this))
                    .workerThreads(4)
                    .selectorThreads(1)
                    .protocolFactory(TCompactProtocol.Factory())
                    .transportFactory(TFramedTransport.Factory())
            args.maxReadBufferBytes = (1024 * 1024 * 10).toLong()
            server = TThreadedSelectorServer(args)
            thread.execute { server!!.serve() }

        } catch (e: TTransportException) {
            throw RuntimeException("Unable to start thrift server " + e, e)
        }

    }


    @Throws(CusterExceptionT::class, TException::class)
    override fun executeTask(task: TaskStartT): TaskResultT? {
        try {
            return processManagerService.executeClusterTask(task)
        } catch (e: Exception) {
            throw CusterExceptionT(1, "execute task failed: " + e.message)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun killTask(kill: TaskKillT) {
        try {
            processManagerService.kill(kill)
        } catch (e: Exception) {
            throw CusterExceptionT(1, "kill task failed: " + e.message)
        }

    }

    @Throws(CusterExceptionT::class, TException::class)
    override fun killAll() {

    }
}
