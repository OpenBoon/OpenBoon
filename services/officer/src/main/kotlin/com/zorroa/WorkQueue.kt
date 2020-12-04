package com.zorroa

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.stackdriver.StackdriverConfig
import io.micrometer.stackdriver.StackdriverMeterRegistry
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.TransportMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.lang.management.ManagementFactory
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object WorkQueue {

    val redis = RedisConfig()
    private val logger = LoggerFactory.getLogger(WorkQueue::class.java)

    fun execute(workQueueEntry: WorkQueueEntry) {

        if (registerRequest(workQueueEntry.request)) {
            redis.pool.execute(workQueueEntry)
        } else {
            logger.warn("request ${workQueueEntry.request.requestId} is already running")
        }
    }

    private fun registerRequest(request: RenderRequest): Boolean {
        return if (redis.redisson != null) {
            try {
                logger.info("Registering request ${request.requestId}")
                redis.redisson.getBucket<String>(request.requestId).trySet(
                    request.outputPath, 5, TimeUnit.MINUTES
                )
            } catch (ex: Exception) {
                logger.warn("Failed to register request with Reddis", ex)
                true
            }
        } else {
            true
        }
    }

    fun updateRequest(request: RenderRequest) {
        if (redis.redisson != null) {
            try {
                redis.redisson.getBucket<String>(request.requestId).set(
                    request.outputPath, 5, TimeUnit.MINUTES
                )
            } catch (e: Exception) {
                logger.warn("Failed to update request with Reddis", e)
            }
        }
    }

    fun unregisterRequest(request: RenderRequest): Boolean {
        return if (redis.redisson != null) {
            try {
                logger.info("Unregistering request ${request.requestId}")
                redis.redisson.getBucket<String>(request.requestId).delete()
            } catch (e: Exception) {
                logger.warn("Failed to unregister with Reddis, assuming file should be processed", e)
                true
            }
        } else {
            true
        }
    }
}

class WorkQueueEntry(
    val document: Document,
    val request: RenderRequest
) : Runnable, Serializable {

    override fun run() {
        // calls close automatically
        document.use {
            logger.info("Rendering ${request.fileName}. Id: ${request.requestId}")
            it.render()
            WorkQueue.unregisterRequest(request)
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(WorkQueueEntry::class.java)
    }
}

class RedisConfig {

    val pool: ThreadPoolExecutor
    val redisson: RedissonClient?

    val queue: BlockingQueue<Runnable>
    private val opsys = System.getProperty("os.name").toLowerCase()
    private val logger = LoggerFactory.getLogger(RedisConfig::class.java)

    init {
        // If the redis host is set then we use a Redis queue.
        var host = System.getenv("REDIS_HOST") ?: System.getProperty("REDIS_HOST")

        if (host != null) {
            val config = org.redisson.config.Config()
            if (opsys == "linux") {
                config.transportMode = TransportMode.EPOLL
            } else {
                config.transportMode = TransportMode.NIO
            }

            config.useSingleServer().address = "redis://$host"

            logger.info("Creating Redis distributed queue")
            redisson = Redisson.create(config)
            queue = redisson.getBlockingQueue("officerQueue")
        } else {
            // Otherwise a regular old queue.
            redisson = null
            queue = LinkedBlockingQueue()
        }

        pool = createThreadPool()
    }

    /**
     * Creates a ThreadPoolExecutor backed by whatever queue type was
     * detected. The number of threads is the same as the number of detected
     * CPUs.
     */
    private fun createThreadPool(): ThreadPoolExecutor {
        val os = ManagementFactory.getOperatingSystemMXBean()
        val threads = os.availableProcessors * 2

        logger.info("Creating thread pool threads=$threads burst=${threads + 1}")
        val pool = ThreadPoolExecutor(threads, threads + 1, 5, TimeUnit.MINUTES, queue)
        ExecutorServiceMetrics.monitor(Metrics.meters, pool, "app_queue", listOf())
        return pool
    }

    object Metrics {

        val meters = getMeterRegistry()

        fun getMeterRegistry(): MeterRegistry {
            val registry = if (System.getenv("project") != null) {
                StackdriverMeterRegistry.builder(InteralStackDriverConfig()).build()
            } else {
                SimpleMeterRegistry()
            }
            registry.config().commonTags("application", "officer")
            JvmMemoryMetrics().bindTo(registry)
            JvmGcMetrics().bindTo(registry)
            ProcessorMetrics().bindTo(registry)
            return registry
        }

        class InteralStackDriverConfig : StackdriverConfig {

            override fun projectId(): String {
                return System.getenv("project") ?: "zorroa-deploy"
            }

            override fun get(p0: String): String? {
                return null
            }
        }
    }
}
