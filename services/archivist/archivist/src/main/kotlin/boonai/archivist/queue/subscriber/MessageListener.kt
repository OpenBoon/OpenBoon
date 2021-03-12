package boonai.archivist.queue.subscriber

abstract class MessageListener : org.springframework.data.redis.connection.MessageListener {

    fun extractOperation(channel: String) = channel.split('/').last()
}