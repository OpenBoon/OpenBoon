package boonai.archivist.queue.subscriber

import boonai.archivist.service.IndexRoutingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("index-routing-listener")
class IndexRoutingListener(
    val indexRoutingService: IndexRoutingService
) : MessageListener() {

    override fun getOptMap() = mapOf(
        "project/close-and-delete" to { projectId: String -> closeAndDeleteProjectIndexes(projectId) }
    )

    fun closeAndDeleteProjectIndexes(projectId: String) {
        try {
            logger.info("Delete project index of Project: $projectId")
            Thread.sleep(1000)
            // indexRoutingService.closeAndDeleteProjectIndexes(UUID.fromString(projectIndex))
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(IndexRoutingListener::class.java)
    }
}
