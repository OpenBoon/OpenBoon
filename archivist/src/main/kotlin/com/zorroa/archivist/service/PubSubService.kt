package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
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
        logger.info("Initializing GCP pub sub {} {}", settings.project, settings.subscription)
        subscription = ProjectSubscriptionName.of(settings.project, settings.subscription)
        subscriber = Subscriber.newBuilder(settings.subscription, GcpDataMessageReceiver())
                .setCredentialsProvider({ GoogleCredentials.fromStream(FileInputStream("$configPath/data-credentials.json")) })
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

            if (type == "UPDATE") {

                val assetId = payload["key"] as String
                val companyId = payload["companyId"] as Int
                val jobName = "$assetId-$type".toLowerCase()

                try {
                    /**
                     * Currently IRM has to name the org company-id for use to pick it up.
                     */
                    val org = organizationDao.get("company-" + payload["companyId"])
                    val asset = Asset(
                            UUID.fromString(assetId),
                            org.id,
                            mutableMapOf("companyId" to companyId))

                    val doc = try {
                        assetService.getDocument(asset)
                    } catch (e: Exception) {
                        logger.warn("Asset ID does not exist: $assetId")
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
                            PipelineType.IMPORT,
                            org.id,
                            zps,
                            lockAssets=true,
                            attrs=mutableMapOf("companyId" to companyId.toString()))

                    val job = jobSerice.launchJob(spec)
                    logger.info("launched job: {}", job)

                } catch (e: Exception) {
                    logger.warn("Error launching job: {}, asset: {} company: {}",
                            e.message, assetId, companyId)
                }
            }
            consumer.ack()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpPubSubServiceImpl::class.java)
    }
}
