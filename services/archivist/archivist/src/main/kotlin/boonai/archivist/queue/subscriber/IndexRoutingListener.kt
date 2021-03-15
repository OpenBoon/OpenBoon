package boonai.archivist.queue.subscriber

import boonai.archivist.service.IndexRoutingService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.stereotype.Service
import java.util.UUID

@Service("index-routing-listener")
class IndexRoutingListener(
    val indexRoutingService: IndexRoutingService
) : MessageListener() {

    override fun onMessage(msg: Message, p1: ByteArray?) {
        val channel = String(msg.channel)
        val content = String(msg.body)
        val opt = extractOperation(channel)

        optMap[opt]?.let {
            it(content)
        }
    }

    fun closeAndDeleteProjectIndexes(projectIndex: String) {
        try {
            indexRoutingService.closeAndDeleteProjectIndexes(UUID.fromString(projectIndex))
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    private val optMap = mapOf(
        "project/close-and-delete" to { projectId: String -> closeAndDeleteProjectIndexes(projectId) }
    )

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingListener::class.java)
    }

}