package boonai.archivist.queue.publisher

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class IndexRoutingPublisherTest() : PublisherTest() {

    @Autowired
    lateinit var indexRoutingPublisher: IndexRoutingPublisher

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
