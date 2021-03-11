package boonai.archivist.queue.publisher

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate

abstract class MessagePubisher {

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, Any>

    abstract fun publish()
}
