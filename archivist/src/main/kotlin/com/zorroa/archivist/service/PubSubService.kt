package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.common.clients.AnalystClient
import com.zorroa.common.domain.Document
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.PipelineType
import com.zorroa.common.domain.ZpsScript
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

interface PubSubService

@Configuration
@ConfigurationProperties("archivist.pubsub.gcs")
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
class GcpPubSubServiceImpl : PubSubService {

    @Autowired
    lateinit var settings: GooglePubSubSettings

    @Autowired
    private lateinit var organizationDao: OrganizationDao

    @Autowired
    private lateinit var indexDao: IndexDao

    @Autowired
    private lateinit var analystClient: AnalystClient

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

        /**
         * This method will create a single job per message.
         */
        override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
            val payload : Map<String, Any> = try {
                Json.Mapper.readValue(message.data.toByteArray())
            } catch (e: Exception) {
                consumer.ack()
                logger.warn("Failed to parse GPubSub payload: {}", String(message.data.toByteArray()))
                return
            }

            // TODO: authorize this thread

            val type= payload["type"] as String?

            if (type == "UPDATE") {

                val assetId = payload["key"] as String
                val companyId = payload["companyId"] as Int
                val jobName = "$assetId-$type".toLowerCase()

                /**
                 * TODO: Fix what the org name is
                 */
                val org = organizationDao.get("irm-" + payload["companyId"])

                // Pull the latest document or otherwise create a new one.
                // TODO: use CDV
                val doc = try {
                    indexDao.get(assetId)
                } catch (e: Exception) {
                    Document(assetId)
                }

                // make sure these are set.
                doc.setAttr("irm.companyId", companyId)
                doc.setAttr("zorroa.organizationId", org.id)

                /**
                 * If the same event comes in for a given job we'll attempt to run
                 * it again, assuming its not already running.
                 */
                try {
                    val zps = ZpsScript(jobName, over=mutableListOf(doc))
                    val spec = JobSpec(jobName,
                            PipelineType.IMPORT,
                            org.id,
                            zps,
                            lockAssets = true,
                            attrs=mutableMapOf("companyId" to companyId.toString()))

                    val job = analystClient.createJob(spec)
                    logger.info("launched job: {}", job)

                } catch (e: Exception) {
                    logger.warn("Error launching job: {}, asset: {} company: {}",
                            e.message, assetId, companyId)
                }
                catch (e: Exception) {

                }
            }
            consumer.ack()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpPubSubServiceImpl::class.java)
    }
}
