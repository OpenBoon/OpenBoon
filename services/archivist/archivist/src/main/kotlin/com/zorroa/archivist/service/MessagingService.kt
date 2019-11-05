package com.zorroa.archivist.service

import com.google.api.core.ApiFuture
import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import com.zorroa.archivist.util.Json
import org.slf4j.LoggerFactory
import java.util.UUID

enum class ActionType(val label: String) {
    AssetsCreated("assets-created"),
    AssetsDeleted("assets-deleted"),
    AssetsUpdated("assets-updated");
}

/**
 * The MessagingService is used to publish messages to a messaging queue such as Google Pub/Sub or RabbitMQ.
 */
interface MessagingService {
    /**
     * Publishes a message to the messaging service.
     *
     * @param[actionType] Action that was taken that prompted this message to be sent.
     * @param[projectId] Action that was taken that prompted this message to be sent.
     * @param[data] Payload of information about the action that was taken.
     */
    fun sendMessage(actionType: ActionType, projectId: UUID?, data: Map<Any, Any>)
}

/**
 * MessagingService implementation that does nothing. This is the default service that is instantiated when a real
 * messaging service is not configured.
 */
class NullMessagingService : MessagingService {
    override fun sendMessage(actionType: ActionType, projectId: UUID?, data: Map<Any, Any>) {}
}

/**
 * MessagingService implementation that uses the Google Pub/Sub service.
 *
 * @param[topicId] ID/name of the Pub/Sub topic to publish messages to.
 */
class PubSubMessagingService constructor(val topicId: String) : MessagingService {
    private val publisher: Publisher

    init {
        val topicName = ProjectTopicName.of(ServiceOptions.getDefaultProjectId(), topicId)
        publisher = Publisher.newBuilder(topicName).build()
        logger.info("Initialized Pub/Sub publisher on $topicId topic.")
    }

    override fun sendMessage(actionType: ActionType, projectId: UUID?, data: Map<Any, Any>) {
        val pubsubMessage = getMessage(actionType, projectId, data)
        publish(pubsubMessage)
    }

    /**
     * Publishes a message. This is a testing seam to allow checking the future that is returned by the publisher.
     *
     * @param[pubsubMessage] Message to publish to the pub/sub topic.
     */
    fun publish(pubsubMessage: PubsubMessage): ApiFuture<String> {
        return publisher.publish(pubsubMessage)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PubSubMessagingService::class.java)

        /**
         * Returns a PubSubMessage ready to be published based on the data and action provided.
         *
         * @param[actionType] Action that was taken that prompted this message to be sent.
         * @param[projectId] UUID of the project this action was taken in.
         * @param[data] Payload of information about the action that was taken.
         * @return PubSubMessage ready to be published.
         */
        fun getMessage(actionType: ActionType, projectId: UUID?, data: Map<Any, Any>): PubsubMessage {
            val dataString = ByteString.copyFromUtf8(Json.serializeToString(data))
            val pubsubMessage = PubsubMessage.newBuilder()
                .setData(dataString)
                .putAttributes("action", actionType.label)
                .putAttributes("projectId", projectId.toString())
                .build()
            return pubsubMessage
        }
    }
}
