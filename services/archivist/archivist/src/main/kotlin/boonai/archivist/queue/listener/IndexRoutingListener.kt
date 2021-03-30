package boonai.archivist.queue.listener

import boonai.archivist.service.IndexRoutingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.listener.Topic
import org.springframework.stereotype.Service
import java.util.UUID

@Service("index-routing-listener")
class IndexRoutingListener(
    val indexRoutingService: IndexRoutingService,
) : MessageListener() {

    @Autowired
    @Qualifier("index-routing-topic")
    lateinit var channel: Topic

    override fun getTopic() = channel

    override fun getOptMap() = mapOf(
        "project/close-and-delete" to { projectId: String -> closeAndDeleteProjectIndexes(projectId) }
    )

    fun closeAndDeleteProjectIndexes(projectId: String) {
        try {
            indexRoutingService.closeAndDeleteProjectIndexes(UUID.fromString(projectId))
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingListener::class.java)
    }
}
