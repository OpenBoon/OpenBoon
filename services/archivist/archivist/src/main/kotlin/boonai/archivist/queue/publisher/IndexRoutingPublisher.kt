package boonai.archivist.queue.publisher

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.listener.Topic
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IndexRoutingPublisher(
    @Qualifier("index-routing-topic") val channel: Topic
) : MessagePubisher(channel) {

    fun closeAndDeleteProject(projectID: UUID) {
        publish("project/close-and-delete", projectID)
    }

}