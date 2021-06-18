package boonai.archivist.service

import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.util.getPubSubEmulationHost
import boonai.archivist.util.isPubSubEmulation
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import javax.annotation.PreDestroy

/**
 * Handles keeping track of all the PubSub publishers.
 */
interface PublisherService {
    fun publish(topic: String, msg: PubsubMessage)
}

@Service
class PublisherServiceImpl : PublisherService {

    val topics = listOf("model-events", "webhooks")
    val publishers: Map<String, Publisher>
    val threads = Executors.newFixedThreadPool(4)

    init {
        publishers = if (!ArchivistConfiguration.unittest) {
            topics.associateWith { createPubSubPublisher(it) }
        } else {
            emptyMap()
        }
    }

    @PreDestroy
    fun shutdown() {
        publishers.values.forEach { it.shutdown() }
    }

    override fun publish(topic: String, msg: PubsubMessage) {
        if (ArchivistConfiguration.unittest) {
            logger.info("Publishing message to topic $topic")
            return
        }
        if (publishers.containsKey(topic)) {
            throw IllegalArgumentException("Invalid pubsub stopic $topic")
        }
        threads.execute {
            publishers.getValue(topic).publish(msg)
        }
    }

    /**
     * Create a Google PubSub Publisher
     */
    fun createPubSubPublisher(topicId: String): Publisher {
        return if (!isPubSubEmulation()) {
            val topicName = ProjectTopicName.of(ServiceOptions.getDefaultProjectId(), topicId)
            Publisher.newBuilder(topicName).build()
        } else {
            val hostport = getPubSubEmulationHost()
            val channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build()
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()

            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            Publisher.newBuilder("projects/localdev/topics/$topicId")
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(PublisherServiceImpl::class.java)
    }
}
