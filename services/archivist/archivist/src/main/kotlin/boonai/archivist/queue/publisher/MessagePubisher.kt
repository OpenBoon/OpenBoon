package boonai.archivist.queue.publisher

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.Topic

abstract class MessagePubisher(
    private val channel: Topic
) {
    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, Any>

    protected fun publish(operation: String, message: Any) {
        redisTemplate.convertAndSend("${channel.topic}/$operation", message)
    }
}
