package boonai.archivist.queue

import boonai.archivist.queue.listener.MessageListener
import boonai.common.util.Json

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import redis.clients.jedis.JedisPool

@Component
class IdleMessagesVerifier {

    @Autowired
    lateinit var jedisPool: JedisPool

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @Scheduled(fixedRate = MessageListener.checkTimeMillis)
    fun findIdleIncompletedTasks() {
        val cache = jedisPool.resource

        val runningTasks = cache.hgetAll(MessageListener.runningTasksKey)

        runningTasks?.forEach { (key, value) ->

            val contentMap = Json.Mapper.readValue(value, Map::class.java)
            val channel = contentMap["channel"] as String
            val content = contentMap["content"] as String

            val existBlockCode = cache.exists(MessageListener.getBlockCode(channel, content))
            val encodedState = cache.get(MessageListener.getEncodedState(channel, content))

            // If there is no blocking code and has not been accomplished
            if (!existBlockCode && encodedState == null) {
                logger.info("Idle tasks found, rerunning")
                redisTemplate.convertAndSend(channel, content)
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(IdleMessagesVerifier::class.java)
    }
}
