package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.QueuedFileSpec
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.FileQueueService
import com.zorroa.archivist.service.PipelineService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

import java.util.*

class FileQueueControllerTests :  MockMvcTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var fileQueueService: FileQueueService

    @Test
    fun testGetOrgMeters() {
        val session = admin()
        val pipeline = pipelineService.create(
                PipelineSpec("foo", PipelineType.Import, "test", listOf()))

        val org = getOrgId()
        val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(), "/tmp/foo.jpg", mapOf("foo" to "bar"))
        fileQueueService.create(spec)

        val req = mvc.perform(MockMvcRequestBuilders.get("/api/v1/file-queue/_meters")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        println(req.response.contentAsString)
    }
}