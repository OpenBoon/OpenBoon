package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

interface PubSubService

@Configuration
@ConfigurationProperties("archivist.pubsub.gcp")
class GooglePubSubSettings {
    lateinit var subscription: String
    lateinit var project: String
    var enabled : Boolean = true
}

@Component
class JobEventSubscription {

    @Autowired
    lateinit var settings: GooglePubSubSettings

    @Autowired
    lateinit var exportService: ExportService

    var subscription : ProjectSubscriptionName? = null
    var subscriber: Subscriber? = null

    @Value("\${archivist.config.path}")
    lateinit var configPath: String

    @PostConstruct
    fun setup() {
        val project = System.getenv("GCLOUD_PROJECT")

        if (project != null) {
            logger.info("Initializing Analyst Job Event pub sub {}", project)
            subscription = ProjectSubscriptionName.of(project, "zorroa-archivist")
            subscriber = Subscriber.newBuilder(subscription, JobEventReceiver())
                    .setCredentialsProvider{ GoogleCredentials.fromStream(FileInputStream("$configPath/data-credentials.json")) }
                    .build()
            subscriber?.let {
                it.startAsync().awaitRunning()
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        subscriber?.let {
            it.stopAsync()
        }
    }

    inner class JobEventReceiver : MessageReceiver {

        override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
            consumer.ack()
            message?.data?.let {
                val event = Json.Mapper.readValue(it.toByteArray(), JobEvent::class.java)
                logger.info("pubsub message: {}", event.type)
                when(event.type) {
                    "job-state-change"-> {
                        val payload = Json.Mapper.convertValue(
                                event.payload, JobStateChangeEvent::class.java)

                        if (payload.job.type == PipelineType.Export) {
                            try {
                                logger.info("Updating export state: {}", payload.job.env)
                                val id = UUID.fromString(payload.job.env["ZORROA_EXPORT_ID"])
                                val job = exportService.get(id)

                            } catch (e: Exception) {
                                logger.warn("faild to update job state from event: ", event.payload)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobEventSubscription::class.java)
    }

}


class NoOpPubSubService : PubSubService {

}

/**
 * A GCP specific EventService built on Google Pub/Sub.  This class currently waits for certain
 * events and launches kubernetes jobs to process data as it comes in.
 */

class GcpPubSubServiceImpl : PubSubService {

    @Autowired
    lateinit var settings: GooglePubSubSettings

    @Autowired
    private lateinit var organizationDao: OrganizationDao

    @Autowired
    private lateinit var jobSerice: JobService

    @Autowired
    private lateinit var assetService: AssetService

    @Value("\${archivist.config.path}")
    lateinit var configPath: String

    lateinit var subscription : ProjectSubscriptionName
    lateinit var subscriber: Subscriber

    @PostConstruct
    fun setup() {
        val project = System.getenv("GCLOUD_PROJECT")
        logger.info("Initializing GCP pub sub {} {}", project, "zorroa-ingest-pipeline")
        subscription = ProjectSubscriptionName.of(project, "zorroa-ingest-pipeline")
        subscriber = Subscriber.newBuilder(subscription, GcpDataMessageReceiver())
                .setCredentialsProvider { GoogleCredentials.fromStream(FileInputStream("$configPath/data-credentials.json")) }
                .build()
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

            val type= payload["type"] as String?
            val time = System.currentTimeMillis() / 1000

            if (type == "UPDATE") {

                val assetId = payload["key"] as String
                val companyId = payload["companyId"] as Int
                val jobName = "$time-$type-$assetId".toLowerCase()

                try {
                    /**
                     * Currently IRM has to name the org company-id for use to pick it up.
                     */
                    val org = organizationDao.get("company-" + payload["companyId"])

                    /**
                     * Set ourselves as the super admin for the given companty.
                     */
                    SecurityContextHolder.getContext().authentication = SuperAdminAuthentication(org.id)

                    // Need all this to query the asset serivce (which would be CDV)
                    val asset = Asset(
                            UUID.fromString(assetId),
                            org.id,
                            mutableMapOf("companyId" to companyId))

                    val doc = try {
                        assetService.getDocument(asset)
                    } catch (e: Exception) {
                        logger.warn("Exiting ES metadata does not exist for $assetId")
                        Document(assetId)
                    }

                    // make sure these are set.
                    doc.setAttr("irm.companyId", companyId)
                    doc.setAttr("zorroa.organizationId", org.id)

                    /**
                     * No ZORROA_USER env var means any communication back to the
                     * archivist with a proper service key will be super admin.
                     */
                    val zps = ZpsScript(jobName, over=mutableListOf(doc))
                    val spec = JobSpec(jobName,
                            PipelineType.Import,
                            org.id,
                            zps,
                            lockAssets=true,
                            attrs=mutableMapOf("companyId" to companyId.toString()))

                    val job = jobSerice.launchJob(spec)
                    logger.info("launched job: {}", job)

                } catch (e: Exception) {
                    logger.warn("Error launching job: {}, asset: {} company: {}",
                            e.message, assetId, companyId, e)
                }
                finally {
                    SecurityContextHolder.getContext().authentication = null
                }
            }
            consumer.ack()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpPubSubServiceImpl::class.java)
    }
}
