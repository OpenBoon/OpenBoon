package boonai.archivist.service

import boonai.archivist.domain.PubSubEvent
import boonai.archivist.util.getPubSubEmulationHost
import boonai.archivist.util.isPubSubEmulation
import com.google.api.gax.core.CredentialsProvider
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.AlreadyExistsException
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.AckReplyConsumer
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.cloud.pubsub.v1.TopicAdminSettings
import com.google.common.eventbus.EventBus
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeoutException
import javax.annotation.PostConstruct
import kotlin.concurrent.thread

/**
 * A service which listens on various PubSub channels and then emits the
 * messages through the local EventBus.
 */
interface SubscriberService

@Component
class PubSubSubscriberService(val eventBus: EventBus) : SubscriberService {

    val subs = listOf("archivist-cloud-builds")

    @PostConstruct
    fun subscribeToAll() {
        /**
         * If we're using PubSub emulation then we gotta setup the test topics.
         */
        if (isPubSubEmulation()) {
            setupTestTopics()
        }

        /**
         * Start all sub listeners.
         */
        for (sub in subs) {
            thread {
                listen(sub)
            }
        }
    }

    /**
     * Build a PubSub Subscriber.
     */
    fun buildSubscriber(sub: String, receiver: MessageReceiver): Subscriber {
        return if (isPubSubEmulation()) {
            val hostport = getPubSubEmulationHost()
            val channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build()
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()

            Subscriber.newBuilder(ProjectSubscriptionName.of("localdev", sub), receiver)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build()
        } else {
            val projectId = ServiceOptions.getDefaultProjectId()
            val subscriptionName = ProjectSubscriptionName.of(projectId, sub)
            Subscriber.newBuilder(subscriptionName, receiver).build()
        }
    }

    fun listen(sub: String) {

        // Instantiate an asynchronous message receiver.
        val receiver = MessageReceiver { message: PubsubMessage, consumer: AckReplyConsumer ->
            eventBus.post(PubSubEvent(message, sub))
            consumer.ack()
        }

        val subscriber = buildSubscriber(sub, receiver)
        try {
            subscriber.startAsync().awaitRunning()
            subscriber.awaitTerminated()
        } catch (timeoutException: TimeoutException) {
            logger.error("Subscription '$sub' timed out.")
            subscriber.stopAsync()
        }
    }

    fun setupTestTopics() {
        // For unittests the
        val hostport = getPubSubEmulationHost()
        val channel = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build()
        try {
            val channelProvider: TransportChannelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
            val credentialsProvider: CredentialsProvider = NoCredentialsProvider.create()

            // Set the channel and credentials provider when creating a `TopicAdminClient`.
            // Similarly for SubscriptionAdminClient
            val topicClient = TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()
            )
            val topicName = TopicName.of("localdev", "cloud-builds")
            try {
                topicClient.createTopic(topicName)
            } catch (ex: AlreadyExistsException) {
                logger.warn(ex.message)
            }

            val subClient = SubscriptionAdminClient.create(
                SubscriptionAdminSettings.newBuilder()
                    .setTransportChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build()
            )

            val subName = ProjectSubscriptionName.of("localdev", "archivist-cloud-builds")

            try {
                subClient.createSubscription(subName, topicName, PushConfig.getDefaultInstance(), 120)
            } catch (ex: AlreadyExistsException) {
                logger.warn(ex.message)
            }
        } finally {
            channel.shutdown()
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PubSubSubscriberService::class.java)

        const val UNITTEST_HOST = "localhost:8085"
    }
}
