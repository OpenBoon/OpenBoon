package boonai.archivist.service

import boonai.archivist.config.ApplicationProperties
import boonai.archivist.domain.Asset
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.repository.ProjectDao
import boonai.archivist.security.getProjectId
import boonai.common.util.Json
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

interface WebHookPublisherService {
    fun emitMessage(asset: Asset, hook: WebHook, trigger: TriggerType)
    fun handleAssetTriggers(asset: Asset, trigger: TriggerType)
    fun testWebHook(wb: WebHook)
    fun testWebHook(spec: WebHookSpec)
    fun makeAssetPayload(asset: Asset, trigger: TriggerType) : String
}

@Service
class WebHookPublisherServiceImpl constructor(
    val webHookService: WebHookService
) : WebHookPublisherService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var projectDao: ProjectDao

    lateinit var publisher: Publisher

    private val webhookCache = CacheBuilder.newBuilder()
        .maximumSize(60)
        .initialCapacity(10)
        .concurrencyLevel(4)
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build(object : CacheLoader<UUID, List<WebHook>>() {
            @Throws(Exception::class)
            override fun load(projectId: UUID): List<WebHook> {
                return webHookService.getActiveWebHooks(projectId)
            }
        })

    @PostConstruct
    fun initialize() {
        publisher = createPublisher()
    }

    override fun handleAssetTriggers(asset: Asset, trigger: TriggerType) {
        for (hook in webhookCache.get(getProjectId())) {
            if (trigger in hook.triggers) {
                emitMessage(asset, hook, trigger)
            }
        }
    }

    override fun emitMessage(asset: Asset, hook: WebHook, trigger: TriggerType) {
        val dataString = makeAssetPayload(asset, trigger)
        emitMessage(asset.id, dataString, hook.url, hook.secretKey, trigger)
    }

    fun emitMessage(assetId: String, dataString: String, url: String, key: String, trigger: TriggerType) {
        val bytes = ByteString.copyFromUtf8(dataString)
        val pubsubMessage = PubsubMessage.newBuilder()
            .setData(bytes)
            .putAttributes("trigger", trigger.name)
            .putAttributes("project_id", getProjectId().toString())
            .putAttributes("url", url)
            .putAttributes("secret_key", key)
            .build()
        publisher.publish(pubsubMessage)
    }

    override fun testWebHook(wb: WebHook) {
        for (trig in wb.triggers) {
            webHookService.validateUrl(wb.url)
            val asset = ClassPathResource("webhooks/${trig.name}.json").inputStream.use {
                Json.Mapper.readValue<Asset>(it)
            }
            emitMessage("00000000-0000-0000-0000-000000000000",
                makeAssetPayload(asset, trig), wb.url, wb.secretKey, trig)
        }
    }

    override fun testWebHook(spec: WebHookSpec) {
        for (trig in spec.triggers) {
            webHookService.validateUrl(spec.url)
            val asset = ClassPathResource("webhooks/${trig.name}.json").inputStream.use {
                Json.Mapper.readValue<Asset>(it)
            }
            emitMessage("00000000-0000-0000-0000-000000000000",
                makeAssetPayload(asset, trig), spec.url, spec.secretKey, trig)
        }
    }

    override fun makeAssetPayload(asset: Asset, trigger: TriggerType) : String {
        val project = projectDao.getById(getProjectId())
        val payload = mapOf(
            "asset" to asset,
            "trigger" to trigger.name,
            "project" to mapOf("id" to project.id, "name" to project.name)
        )
        return Json.serializeToString(payload)
    }

    private fun createPublisher(): Publisher {

        var topicId: String = properties.getString("boonai.webhooks.topic-name")
        val hostport = System.getenv("PUBSUB_EMULATOR_HOST")
        return if (hostport == null) {
            val topicName = ProjectTopicName.of(ServiceOptions.getDefaultProjectId(), topicId)
            Publisher.newBuilder(topicName).build()
        } else {
            logger.info("Utilizing PubSub emulator!")
            val channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build()
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()

            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            Publisher.newBuilder("projects/localdev/topics/webhooks")
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebHookPublisherServiceImpl::class.java)
    }
}
