package boonai.archivist.queue.subscriber

import boonai.archivist.service.ProjectService
import boonai.archivist.service.ProjectServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.Message
import org.springframework.stereotype.Service
import java.util.UUID

@Service("project-listener")
class ProjectListener : MessageListener() {

    @Autowired
    lateinit var projectService: ProjectService

    override fun onMessage(msg: Message, p1: ByteArray?) {
        val channel = String(msg.channel)
        val content = String(msg.body)
        val opt = extractOperation(channel)

        optMap[opt]?.let {
            it(content)
        }
    }

    private val optMap = mapOf(
        "delete" to { content: String -> delete(content) },
        "system-storage/delete" to { content: String -> deleteProjectSystemStorage(content) },
        "storage/delete" to { content: String -> deleteProjectStorage(content) }
    )

    private fun delete(content: String) {
        try {
            val projectId = UUID.fromString(content)
            val project = projectService.get(projectId)
            projectService.delete(project)
            logger.debug("Deleting project $projectId")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    private fun deleteProjectStorage(content: String) {
        try {
            val projectId = UUID.fromString(content)
            val project = projectService.get(projectId)
            projectService.deleteProjectStorage(project)
            logger.debug("Deleting project:$projectId Storage")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    fun deleteProjectSystemStorage(content: String) {
        try {
            val projectId = UUID.fromString(content)
            val project = projectService.get(projectId)
            projectService.deleteProjectSystemStorage(project)
            logger.debug("Deleting Project:$projectId System Storage")
        } catch (ex: IllegalArgumentException) {
            logger.error("Bad content format")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
    }
}
