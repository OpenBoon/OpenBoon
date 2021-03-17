package boonai.archivist.queue.publisher

import boonai.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class IndexRoutingPublisherTest() : AbstractTest() {

    @Autowired
    lateinit var indexRoutingPublisher: IndexRoutingPublisher

    @Test
    fun testCloseAndDeleteProject() {
        val randomId = UUID.randomUUID()
        redisListener.addMessageListener(

            { message, _ ->
                assertEquals(randomId, UUID.nameUUIDFromBytes(message.body))
            },
            indexRoutingPublisher.channel
        )

        indexRoutingPublisher.closeAndDeleteProject(UUID.randomUUID())
    }
}
