package boonai.archivist.service

import com.google.protobuf.ByteString
import boonai.archivist.AbstractTest
import org.junit.Ignore
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessagingServiceTests : AbstractTest() {

    @Test
    fun testgetMessage() {
        val data: Map<Any, Any> = mapOf("id" to 1)
        val message = PubSubMessagingService.getMessage(
            actionType = ActionType.AssetsDeleted,
            projectId = UUID.randomUUID(),
            data = data
        )
        assertEquals(expected = ActionType.AssetsDeleted.label, actual = message.getAttributesOrThrow("action"))
        assertEquals(expected = ByteString.copyFromUtf8("{\"id\":1}"), actual = message.data)
    }

    @Test
    @Ignore(
        "This integration test publishes a real message to a pub/sub queue. To use this test you must be " +
            "authenticated using the gcloud sdk and have your project set to a project that has an archivist-events-test " +
            "topic. You can use the `gcloud auth application-default login` command to authenticate."
    )
    fun testPubSubIntegrationTest() {
        var pubSubMessagingService = PubSubMessagingService(properties)

        val data: Map<Any, Any> = mapOf("id" to 1)
        val message = PubSubMessagingService.getMessage(
            actionType = ActionType.AssetsDeleted,
            projectId = UUID.randomUUID(),
            data = data
        )
        val future = pubSubMessagingService.publish(message)
        val result = future.get()
        assertTrue(future.isDone)
    }
}
