package boonai.archivist.queue.publisher

import org.slf4j.LoggerFactory
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

    fun deleteSystemStorage(projectId: UUID) {
        publish("system-storage/delete", projectId)
    }

    fun deleteStorage(projectId: UUID) {
        publish("storage/delete", projectId)
    }

    fun deleteApiKey(projectId: UUID) {
        publish("api-key/delete", projectId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectPublisher::class.java)
    }
}
