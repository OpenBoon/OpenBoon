package com.zorroa.analyst.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.Maps
import com.google.common.util.concurrent.AbstractScheduledService
import com.zorroa.analyst.isUnitTest
import com.zorroa.cluster.client.MasterServerClient
import com.zorroa.cluster.thrift.AnalystT
import com.zorroa.common.config.ApplicationProperties
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.sdk.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.endpoint.InfoEndpoint
import org.springframework.boot.actuate.endpoint.MetricsEndpoint
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.*
import javax.annotation.PreDestroy

interface RegisterService {

    fun register(url: String): String?
}

@Component
class RegisterServiceImpl @Autowired constructor(
        private val properties: ApplicationProperties,
        private val metrics: MetricsEndpoint,
        private val analyzeThreadPool: Executor,
        private val processManagerService: ProcessManagerService,
        private val networkEnvironment: NetworkEnvironment,
        private val infoEndpoint: InfoEndpoint
) : AbstractScheduledService(), RegisterService, ApplicationListener<ContextRefreshedEvent> {

    private var id: String? = null
    private val registerPool: ExecutorService
    private val queue: BlockingQueue<Runnable>
    private val connected = Maps.newConcurrentMap<String, ConnectionState>()

    private enum class ConnectionState {
        SUCCESS,
        FAIL
    }

    init {
        /**
         * Setup a bounded threadpool for pinging archivists
         */
        queue = LinkedBlockingDeque(250)
        registerPool = ThreadPoolExecutor(4, 4, 300,
                TimeUnit.SECONDS, queue, ThreadPoolExecutor.AbortPolicy())

    }

    override fun onApplicationEvent(contextRefreshedEvent: ContextRefreshedEvent) {
        determineUniqueId()
        startAsync()
    }

    fun determineUniqueId() {
        id = UUID.nameUUIDFromBytes(networkEnvironment.publicUri.toASCIIString().toByteArray()).toString()
        if (id == null) {
            throw RuntimeException("Unable to determine analyst ID")
        }

        System.setProperty("analyst.id", id)
        logger.info("Analyst ID: {}", id)
    }

    override fun runOneIteration() {
        if (isUnitTest) {
            return
        }

        try {
            val urls = properties.getList("analyst.master.host")
            for (url in urls) {
                try {
                    registerPool.execute { register(url) }
                } catch (e: Exception) {
                    logger.warn("Unable to queue archivist register command, queue is full, {}", e.message)
                }

            }
        } catch (e: Exception) {
            logger.warn("Unable to determine archivist master list: {}", e.message, e)
        }

    }

    @Throws(Exception::class)
    override fun shutDown() {
        // TODO: set the state of the analyst to shutdown
    }

    override fun register(url: String): String? {

        try {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val e = analyzeThreadPool as ThreadPoolExecutor

            val mdata = metrics.invoke()
            val fixedMdata = Maps.newHashMapWithExpectedSize<String, Any>(mdata.size)
            metrics.invoke().forEach { k, v -> fixedMdata.put(k.replace('.', '_'), v) }

            val builder = AnalystT()
            builder.setOs(String.format("%s-%s", osBean.name, osBean.version))
            builder.setThreadCount(properties.getInt("analyst.executor.threads"))
            builder.setArch(osBean.arch)
            builder.isData = properties.getBoolean("analyst.index.data")
            builder.setUrl(networkEnvironment.clusterAddr)
            builder.setUpdatedTime(System.currentTimeMillis())
            builder.setQueueSize(e.queue.size)
            builder.setThreadsUsed(e.activeCount)
            builder.setTaskIds(ImmutableList.of())
            builder.setId(id)
            builder.setTaskIds(processManagerService.getTaskIds())
            builder.setLoadAvg(osBean.systemLoadAverage)
            builder.setMetrics(Json.serialize(fixedMdata))
            builder.setVersion(infoEndpoint.invoke()["version"] as String)

            val state = connected[url]
            val client = MasterServerClient(url)
            try {
                client.maxRetries = 0
                client.connectTimeout = 2000
                client.socketTimeout = 2000
                client.ping(builder)
                if (ConnectionState.SUCCESS != state) {
                    logger.info("Registered with {}", url)
                    connected.put(url, ConnectionState.SUCCESS)
                }

            } catch (ex: Exception) {
                if (ConnectionState.FAIL != state) {
                    logger.warn("No connection to archivist {}", url)
                    connected.put(url, ConnectionState.FAIL)
                }
            } finally {
                client.close()
            }
        } catch (e: Exception) {
            logger.warn("Failed to register with {}", url, e)
        }

        return id
    }

    @PreDestroy
    fun shutdown() {
        logger.info("Shutting down registration")
        stopAsync()
    }

    override fun scheduler(): AbstractScheduledService.Scheduler {
        return AbstractScheduledService.Scheduler.newFixedDelaySchedule(
                5, 30, TimeUnit.SECONDS)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(RegisterServiceImpl::class.java)
    }
}

