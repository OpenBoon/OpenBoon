package boonai.archivist.queue.listener

import org.springframework.beans.factory.annotation.Autowired
import redis.clients.jedis.JedisPool
import java.util.Base64
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.listener.Topic

abstract class MessageListener : MessageListener {

    private val expirationTimeSeconds = 1800L
    private val expirationTimeMillis = expirationTimeSeconds * 1000L

    @Autowired
    lateinit var jedisPool: JedisPool

    enum class QueueState {
        BLOCK,
        RUNNING,
        ACCOMPLISHED
    }

    abstract fun getOptMap(): Map<String, (String) -> Unit>

    abstract fun getTopic(): Topic

    override fun onMessage(msg: Message, p1: ByteArray?) {
        val channel = String(msg.channel)
        val content = String(msg.body)
        val cache = jedisPool.resource
        val taskState = getEncodedState(channel, content)
        val blockCode = getBlockCode(channel, content)

        // If it's running in other service wait
        while (cache.incr(blockCode) != 1L && !isAccomplished(taskState))
            Thread.sleep(expirationTimeMillis / 2)

        // If it dies somewhere the others process will have the chance to process
        cache.expire(blockCode, expirationTimeSeconds.toInt())

        while (isRunning(taskState))
            Thread.sleep(expirationTimeMillis)

        // If someone already processed it, then exit
        if (isAccomplished(taskState)) {
            return
        }
        // Otherwise run
        getOptMap()[extractOpt(channel)]?.let {
            cache.setex(taskState, expirationTimeSeconds.toInt(), QueueState.RUNNING.name)
            it(content)
        }
        cache.set(taskState, QueueState.ACCOMPLISHED.name)
    }

    private fun isAccomplished(encodedState: String) =
        jedisPool.resource.get(encodedState) == QueueState.ACCOMPLISHED.name

    private fun isRunning(encodedState: String) =
        jedisPool.resource.get(encodedState) == QueueState.RUNNING.name

    private fun extractOpt(channel: String): String {
        return channel.substringAfter("*/", "/")
    }

    private fun getEncodedState(channel: String, content: String): String {
        return Base64.getEncoder().encodeToString("$channel $content".toByteArray())
    }

    private fun getBlockCode(channel: String, content: String): String {
        return Base64.getEncoder().encodeToString("$channel $content ${QueueState.BLOCK}".toByteArray())
    }
}
