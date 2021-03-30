package boonai.archivist.queue.listener

import boonai.archivist.domain.ProjectFilter
import boonai.archivist.queue.PubSubAbstractTest
import boonai.archivist.security.getProjectId
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.EmptyResultDataAccessException

class MessageListenerTest : PubSubAbstractTest() {

    @Test
    fun testSendMultipleRepeatedMessage() {
        for (i in 0..10)
            sendTestMessage(projectListener, "delete", getProjectId().toString())

        assertThrows<EmptyResultDataAccessException> {
            projectService.findOne(ProjectFilter(ids = listOf(getProjectId())))
        }
    }
}
