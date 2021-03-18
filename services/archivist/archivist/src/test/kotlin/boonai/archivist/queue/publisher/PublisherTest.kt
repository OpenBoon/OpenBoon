package boonai.archivist.queue.publisher

import boonai.archivist.AbstractTest
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.listener.Topic
import java.util.concurrent.Phaser

class PublisherTest : AbstractTest() {

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
        listenerWithValidations: (message: Message) -> Any
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
            listener, channel
        )

        publishingMethod()
        ph.arriveAndAwaitAdvance()
        error?.let { throw (it) }
        redisListener.removeMessageListener(listener, channel)
    }
}
