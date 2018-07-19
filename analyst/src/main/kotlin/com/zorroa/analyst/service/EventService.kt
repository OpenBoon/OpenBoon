package com.zorroa.analyst.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

interface EventService

@Configuration
@ConfigurationProperties("gcp.pubsub")
class GooglePubSubSettings {
    lateinit var subscription: String
    lateinit var project: String
    var enabled : Boolean = true
}

/**
 * A GCP specific EventService built on Google Pub/Sub.  This class currently waits for certain
 * events and launches kubernetes jobs to process data as it comes in.
 */
@Service
class GcpEventServiceImpl : EventService {

    @Autowired
    lateinit var settings: GooglePubSubSettings

    @Autowired
    private lateinit var schedulerService: SchedulerService

    @Autowired
    private lateinit var jobService: JobService

    @Autowired
    private lateinit var pipelineService: PipelineService

    lateinit var subscription : ProjectSubscriptionName
    lateinit var subscriber: Subscriber

    @PostConstruct
    fun setup() {
        subscription = ProjectSubscriptionName.of(settings.project, settings.subscription)
        subscriber = Subscriber.newBuilder(settings.subscription, GcpDataMessageReceiver()).build()
        if (settings.enabled) {
            subscriber.startAsync().awaitRunning()
        }
    }

    @PreDestroy
    fun shutdown() {
        if (settings.enabled) {
            subscriber.stopAsync()
        }
    }

    inner class GcpDataMessageReceiver : MessageReceiver {
        override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
            val payload : Map<String, Any> = try {
                Json.Mapper.readValue(message.data.toByteArray())
            } catch (e: Exception) {
                consumer.ack()
                logger.warn("Failed to parse GPubSub payload: {}", String(message.data.toByteArray()))
                return
            }

            val type= payload["type"] as String?

            if (type == "UPDATE") {
                val assetId = payload["key"] as String
                val companyId = payload["companyId"] as Int
                val jobName = "$assetId-$type".toLowerCase()

                /**
                 * If the same event comes in for a given job we'll attempt to run
                 * it again, assuming its not already running.
                 */
                try {
                    val spec = JobSpec(jobName,
                            UUID.fromString(assetId),
                            UUID.fromString("00000000-9998-8888-7777-666666666666"),
                            pipelineService.getDefaultPipelineList(),
                            attrs=mapOf("companyId" to companyId))

                    val job = jobService.create(spec)
                    logger.info("Created job {} {}", job.id, job.pipelines)
                } catch (e: DuplicateKeyException) {
                    val job = jobService.get(jobName)
                }
                catch (e: Exception) {
                    logger.warn("Error launching job: {}, asset: {} company: {}",
                            e.message, assetId, companyId)
                }
            }
            consumer.ack()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpEventServiceImpl::class.java)
    }
}
