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
import com.zorroa.archivist.util.event
import com.zorroa.common.clients.CoreDataVaultClient
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.dao.EmptyResultDataAccessException
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

interface PubSubService

@Configuration
@ConfigurationProperties("archivist.pubsub.gcp")
class GooglePubSubSettings {
    lateinit var subscription: String
    lateinit var project: String
}

/**
 * A GCP specific EventService built on Google Pub/Sub.  This class currently waits for certain
 * events and launches kubernetes jobs to process data as it comes in.
 */

class GcpPubSubServiceImpl constructor(private val coreDataVaultClient: CoreDataVaultClient) : PubSubService {

    @Autowired
    lateinit var settings: GooglePubSubSettings

    @Autowired
    private lateinit var pipelineService: PipelineService

    @Autowired
    private lateinit var organizationDao: OrganizationDao

    @Autowired
    private lateinit var fileQueueService: FileQueueService

    @Value("\${archivist.config.path}")
    lateinit var configPath: String

    private lateinit var subscription : ProjectSubscriptionName
    private lateinit var subscriber: Subscriber

    @PostConstruct
    fun setup() {
        logger.info("Initializing GCP pub sub {} {}", settings.project, settings.subscription)
        subscription = ProjectSubscriptionName.of(settings.project, settings.subscription)
        val credPath = "$configPath/data-credentials.json"

        val builder = Subscriber.newBuilder(subscription, GcpDataMessageReceiver())
        if (Files.exists(Paths.get(credPath))) {
            builder.setCredentialsProvider { GoogleCredentials.fromStream(FileInputStream(credPath)) }
        }
        subscriber = builder.build()
        subscriber.startAsync().awaitRunning()
    }

    @PreDestroy
    fun shutdown() {
        subscriber.stopAsync()
    }

    inner class GcpDataMessageReceiver : MessageReceiver {

        /**
         * This method will create a single job per message.
         */
        override fun receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer) {
            try {
                val payload : Map<String, Any?> = Json.Mapper.readValue(message.data.toByteArray())
                val type= payload["type"] as String?
                when(type) {
                    "UPDATE" -> handleUpdate(payload)
                }
            } catch (e: Exception) {
                logger.warn("Failed to parse GPubSub payload: {}", String(message.data.toByteArray()))
            }
            finally {
                consumer.ack()
            }
        }

        fun getPipelineId(companyId: Int): UUID {
            /**
             * Tries to get a pipeline based on the companyId and falls back to the standard-import pipeline.
             */
            val pipeline = try {
                pipelineService.get("company-$companyId")
            } catch (e: EmptyResultDataAccessException) {
                pipelineService.get("standard-import")
            }
            return pipeline.id
        }

        fun handleUpdate(payload : Map<String, Any?>) {

            try {
                val assetId = payload.getValue("key").toString()
                val companyId = payload.getValue("companyId") as Int
                val org = organizationDao.get("company-" + payload["companyId"])

                logger.event("pubsub UPDATE",
                        mapOf("companyId" to companyId, "assetId" to assetId, "orgId" to org.id))

                logger.info("PubSub Payload")
                logger.info(Json.prettyString(payload))

                /**
                 * Grab the existing doc, otherise make a new one.
                 */
                val doc = try {
                    coreDataVaultClient.getIndexedMetadata(companyId, assetId)
                } catch (e: Exception) {
                    Document(assetId)
                }

                doc.setAttr("system.organizationId", org.id)
                doc.setAttr("tmp.copy_attrs_to_clip", listOf("irm"))

                val md = coreDataVaultClient.getAsset(companyId, assetId)
                val url =  md["imageURL"].toString().replace("https://storage.cloud.google.com/",
                        "gs://", true)

                // Add IRM metadata
                doc.setAttr("irm.companyId", companyId)
                if (md.containsKey("barcode")) {
                    doc.setAttr("irm.barcode", md["barcode"])
                }
                if (md.containsKey("attributeValues")) {
                    val attributeValues = md["attributeValues"] as List<Map<String, Any>>
                    for (attributeValue in attributeValues) {
                        doc.setAttr("irm." + attributeValue["name"], attributeValue["value"])
                    }
                }

                // queue up the file for processing
                fileQueueService.create(QueuedFileSpec(
                        org.id,
                        getPipelineId(companyId),
                        UUID.fromString(assetId),
                        url,
                        doc.document))

            } catch (e: Exception) {
                logger.warn("Error queueing file: {}", e.message, e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpPubSubServiceImpl::class.java)
    }
}
