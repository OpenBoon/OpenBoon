package boonai.archivist.queue.publisher

import boonai.archivist.queue.PubSubAbstractTest
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

class IndexRoutingPublisherTest : PubSubAbstractTest() {

    @Test
    fun testCloseAndDeleteProject() {
        val randomId = UUID.randomUUID()

        testPublishingMethod(
            indexRoutingPublisher.channel,
            {
                indexRoutingPublisher.closeAndDeleteProject(randomId)
            },
            { message ->
                assertEquals(randomId.toString(), String(message.body))
            }
        )
    }
}
