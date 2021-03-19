package boonai.archivist.queue.listener

import boonai.archivist.service.IndexRoutingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service("index-routing-listener")
class IndexRoutingListener(
    val indexRoutingService: IndexRoutingService,
) : MessageListener() {

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
