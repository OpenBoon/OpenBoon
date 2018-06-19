package com.zorroa.irm.studio.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.irm.studio.Json
import com.zorroa.irm.studio.domain.JobSpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
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
            val string = String(message.data.toByteArray())
            val payload : Map<String, Any> = Json.Mapper.readValue(string)

            val type= payload.get("type") as String
            val assetId = payload.get("key") as String

            if (type == "CREATE") {
                try {
                    val jobName = "$assetId-$type".toLowerCase()
                    val spec = JobSpec(jobName,
                            UUID.fromString(assetId),
                            UUID.fromString("00000000-9998-8888-7777-666666666666"),
                            pipelineService.getDefaultPipelineList())

                    val job = jobService.create(spec)
                    logger.info("Created job {} {}", job.id, job.pipelines)
                    jobService.start(job)
                } catch (e: Exception) {
                    logger.warn("Error launching job:", e)
                }
            }
            consumer.ack()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpEventServiceImpl::class.java)
    }
}
