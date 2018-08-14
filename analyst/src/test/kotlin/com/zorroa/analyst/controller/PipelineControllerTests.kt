package com.zorroa.analyst.controller

import com.zorroa.analyst.AbstractMvcTest
import com.zorroa.analyst.service.PipelineService
import com.zorroa.common.domain.Pipeline
import com.zorroa.common.domain.PipelineType
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@WebAppConfiguration
class PipelineControllerTests : AbstractMvcTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    @Test
    fun testGet() {
        val defaultImport = pipelineService.getDefaultPipelineNames(PipelineType.Import)
        assertTrue(defaultImport.isNotEmpty())

        val req = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/pipelines/" + defaultImport[0])
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val kpl = Json.deserialize(req.response.contentAsByteArray, Pipeline::class.java)
        assertEquals("import-test", kpl.name)
        assertEquals(1, kpl.processors.size)
    }
}
