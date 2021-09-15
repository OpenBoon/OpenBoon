package boonai.archivist.queue.listener

import boonai.archivist.service.ProjectService
import boonai.common.apikey.AuthServerClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.listener.Topic
import org.springframework.stereotype.Service
import java.util.UUID

@Service("project-listener")
class ProjectListener(
    val projectService: ProjectService,
    val authServerClient: AuthServerClient
) : MessageListener() {

    @Autowired
    @Qualifier("index-routing-topic")
    lateinit var channel: Topic

    override fun getTopic() = channel

    override fun getOptMap() = mapOf(
        "delete" to { content: String -> delete(content) },
        "system-storage/delete" to { content: String -> deleteProjectSystemStorage(content) },
        "storage/delete" to { content: String -> deleteProjectStorage(content) },
        "api-key/delete" to { content: String -> deleteApiKeys(content) }
    )

    private fun delete(content: String) {
        try {
            projectService.delete(UUID.fromString(content))
            logger.debug("Deleting project $content")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    private fun deleteProjectStorage(content: String) {
        try {
            val projectId = UUID.fromString(content)
            projectService.deleteProjectStorage(projectId)
            logger.debug("Deleting project:$projectId Storage")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    private fun deleteProjectSystemStorage(content: String) {
        try {
            val projectId = UUID.fromString(content)
            projectService.deleteProjectSystemStorage(projectId)
            logger.debug("Deleting Project:$projectId System Storage")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    private fun deleteApiKeys(content: String) {
        try {
            val projectId = UUID.fromString(content)
            authServerClient.deleteProjectApiKeys(projectId)
            logger.debug("Deleting Project:$projectId API Keys")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectListener::class.java)
    }
}
