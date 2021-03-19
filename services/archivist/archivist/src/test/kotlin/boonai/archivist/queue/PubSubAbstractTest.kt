package boonai.archivist.queue

import boonai.archivist.AbstractTest
import boonai.archivist.queue.listener.IndexRoutingListener
import boonai.archivist.queue.publisher.IndexRoutingPublisher
import boonai.archivist.queue.publisher.ProjectPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.listener.Topic
import java.util.concurrent.Phaser

class PubSubAbstractTest : AbstractTest() {

    @Autowired
    lateinit var indexRoutingPublisher: IndexRoutingPublisher

    @Autowired
    lateinit var indexRoutingListener: IndexRoutingListener

    @Autowired
    lateinit var projectPublisher: ProjectPublisher

    /**
     * This Publish an message on a redis pub/sub channel and waits until the message arrive
     * It's an async process, so this method waits until the message arrives and is validated
     *
     * @param channel Channel that will be used to publish a message
     * @param publishingMethod Method that will call publish method
     * @param listenerWithValidations Method that will receive a method and validate it content
     */
    fun testPublishingMethod(
        channel: Topic,
        publishingMethod: () -> Any,
        listenerWithValidations: (message: Message) -> Any,
    ) {
        val ph = Phaser(2)
        var error: AssertionError? = null

        val listener: (Message, ByteArray?) -> Unit = { message, _ ->
            try {
                listenerWithValidations(message)
            } catch (ex: AssertionError) {
                error = ex
            } finally {
                ph.arrive()
            }
        }
        redisListener.addMessageListener(
            listener,
            channel
        )

        publishingMethod()
        ph.arriveAndAwaitAdvance()
        error?.let { throw (it) }
        redisListener.removeMessageListener(listener, channel)
    }
}
