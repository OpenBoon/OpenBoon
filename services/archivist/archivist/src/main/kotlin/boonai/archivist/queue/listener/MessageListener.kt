package boonai.archivist.queue.listener

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import redis.clients.jedis.JedisPool
import java.util.Base64
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.listener.Topic
import org.springframework.stereotype.Service
import redis.clients.jedis.Jedis
import java.lang.Exception

@Service
abstract class MessageListener : MessageListener {

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

        // Store backup info in redis
        cache.hset(runningTasksKey, blockCode, getTaskInfo(channel, content))

        // Otherwise run
        val statusRefreshCoroutine = runRefreshCoroutine(cache, taskState, blockCode)
        try {
            getOptMap()[extractOpt(channel)]?.let {
                it(content)
            }
        } catch (ex: Exception) {
            logger.error(ex.localizedMessage)
        } finally {
            // Stop refresh coroutine
            statusRefreshCoroutine.cancel()
            // Update Redis status
            cache.set(taskState, QueueState.ACCOMPLISHED.name)
            cache.hdel(runningTasksKey, blockCode)
        }
    }

    /**
     * This method allow that jobs that would exceed processing time keep running
     */
    private fun runRefreshCoroutine(cache: Jedis, taskState: String, blockCode: String) = GlobalScope.launch {
        while (isActive) {
            cache.expire(taskState, expirationTimeSeconds.toInt())
            cache.expire(blockCode, expirationTimeSeconds.toInt())
            Thread.sleep(expirationTimeMillis / 2)
        }
    }

    private fun getTaskInfo(channel: String, content: String): String {
        return boonai.common.util.Json.serializeToString(
            mapOf(
                "channel" to channel,
                "content" to content
            )
        )
    }

    private fun isAccomplished(encodedState: String) =
        jedisPool.resource.get(encodedState) == QueueState.ACCOMPLISHED.name

    private fun isRunning(encodedState: String) =
        jedisPool.resource.get(encodedState) == QueueState.RUNNING.name

    private fun extractOpt(channel: String): String {
        return channel.substringAfter("*/", "/")
    }

    companion object {
        const val expirationTimeSeconds = 1800L
        const val expirationTimeMillis = expirationTimeSeconds * 1000L
        const val checkTimeMillis = expirationTimeMillis / 2
        const val runningTasksKey = "running-tasks"
        val logger = LoggerFactory.getLogger(MessageListener::class.java)

        fun getEncodedState(channel: String, content: String): String {
            return Base64.getEncoder().encodeToString("$channel $content".toByteArray())
        }

        fun getBlockCode(channel: String, content: String): String {
            return Base64.getEncoder().encodeToString("$channel $content ${QueueState.BLOCK}".toByteArray())
        }
    }
}
