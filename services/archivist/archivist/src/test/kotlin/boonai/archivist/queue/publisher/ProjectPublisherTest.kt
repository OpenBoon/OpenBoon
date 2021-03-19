package boonai.archivist.queue.publisher

import boonai.archivist.queue.PubSubAbstractTest
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

class ProjectPublisherTest : PubSubAbstractTest() {

    @Test
    fun projectDeletePublishTest() {
        val randomId = UUID.randomUUID()

        testPublishingMethod(
            projectPublisher.channel,
            {
                projectPublisher.delete(randomId)
            },
            { message ->
                val channelContent = String(message.channel)
                assertEquals(true, channelContent.endsWith("/delete"))
                assertEquals(randomId.toString(), String(message.body))
            }
        )
    }

    @Test
    fun deleteProjectStoragePublishTest() {
        val randomId = UUID.randomUUID()

        testPublishingMethod(
            projectPublisher.channel,
            {
                projectPublisher.deleteStorage(randomId)
            },
            { message ->
                val channelContent = String(message.channel)
                assertEquals(true, channelContent.endsWith("storage/delete"))
                assertEquals(randomId.toString(), String(message.body))
            }
        )
    }

    @Test
    fun deleteProjectSystemStoragePublishTest() {
        val randomId = UUID.randomUUID()

        testPublishingMethod(
            projectPublisher.channel,
            {
                projectPublisher.deleteSystemStorage(randomId)
            },
            { message ->
                val channelContent = String(message.channel)
                assertEquals(true, channelContent.endsWith("system-storage/delete"))
                assertEquals(randomId.toString(), String(message.body))
            }
        )
    }

    @Test
    fun deleteProjectApiKey() {
        val randomId = UUID.randomUUID()

        testPublishingMethod(
            projectPublisher.channel,
            {
                projectPublisher.deleteApiKey(randomId)
            },
            { message ->
                val channelContent = String(message.channel)
                assertEquals(true, channelContent.endsWith("api-key/delete"))
                assertEquals(randomId.toString(), String(message.body))
            }
        )
    }
}
