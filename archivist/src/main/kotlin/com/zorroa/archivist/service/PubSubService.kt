package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.OrganizationDao
import com.zorroa.common.clients.CoreDataVaultClient
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
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

/**
 * A GCP specific EventService built on Google Pub/Sub.  This class currently waits for certain
 * events and launches kubernetes jobs to process data as it comes in.
 */

class GcpPubSubServiceImpl : PubSubService {

    @Autowired
    lateinit var settings: GooglePubSubSettings

    @Autowired
    private lateinit var pipelineService: PipelineService

    @Autowired
    private lateinit var organizationDao: OrganizationDao

    @Autowired
    private lateinit var fileQueueService: FileQueueService

    @Autowired
    private lateinit var coreDataVaultClient: CoreDataVaultClient

    @Value("\${archivist.config.path}")
    lateinit var configPath: String

    private lateinit var subscription : ProjectSubscriptionName
    private lateinit var subscriber: Subscriber

    @PostConstruct
    fun setup() {
        logger.info("Initializing GCP pub sub {} {}", settings.project, settings.subscription)
        subscription = ProjectSubscriptionName.of(settings.project, settings.subscription)
        subscriber = Subscriber.newBuilder(settings.subscription, GcpDataMessageReceiver())
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

            if (type == "UPDATE") {

                val assetId = UUID.fromString(payload["key"] as String)
                val companyId = payload["companyId"] as Int

                try {
                    /**
                     * Currently IRM has to name the org company-id for use to pick it up.
                     */
                    val org = organizationDao.get("company-" + payload["companyId"])
                    val doc = try {
                        coreDataVaultClient.getIndexedMetadata(companyId, assetId)
                    } catch (e: Exception) {
                        logger.warn("Asset ID does not exist: $assetId")
                        Document(assetId.toString())
                    }

                    doc.setAttr("irm.companyId", companyId)
                    doc.setAttr("zorroa.organizationId", org.id)

                    // obtain the file's download path
                    val md = coreDataVaultClient.getMetadata(companyId, assetId)
                    val url =  md["imageURL"].toString().replace("https://storage.cloud.google.com/",
                            "gs://", true)

                    // queue up the file for processing
                    fileQueueService.create(QueuedFileSpec(
                            org.id,
                            assetId,
                            pipelineService.get("full").id,
                            url,
                            doc.document))

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
