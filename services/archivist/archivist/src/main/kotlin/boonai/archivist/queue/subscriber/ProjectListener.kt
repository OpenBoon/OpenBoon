package boonai.archivist.queue.subscriber

import boonai.archivist.service.ProjectServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener

class ProjectListener : MessageListener{

    override fun onMessage(p0: Message, p1: ByteArray?) {
        logger.debug("Message received ${p0.body}")
        logger.debug("Channel ${p0.channel}")
        logger.debug("Content ${String(p1?:"Vazio".toByteArray())}")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
    }
}