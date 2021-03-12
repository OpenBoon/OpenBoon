package boonai.archivist.queue.publisher

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.listener.Topic
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectPublisher(
    @Qualifier("project-topic") val channel: Topic
) : MessagePubisher(channel) {

    fun delete(projectId: UUID) {
        publish("delete", projectId)
    }
}
