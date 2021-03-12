package boonai.archivist.queue.subscriber

import boonai.archivist.service.ProjectService
import boonai.archivist.service.ProjectServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.Message
import org.springframework.stereotype.Service
import java.util.*

@Qualifier("project-listener")
@Service
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
        "delete" to { content: String -> delete(content) }
    )

    fun delete(content: String) {
        val projectId = UUID.fromString(content)
        logger.debug("Deleting project $projectId")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
    }
}