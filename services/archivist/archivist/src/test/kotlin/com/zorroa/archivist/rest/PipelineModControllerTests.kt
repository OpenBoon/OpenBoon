package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.OpFilter
import com.zorroa.archivist.domain.OpFilterType
import com.zorroa.archivist.domain.ModOp
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class PipelineModControllerTests : MockMvcTest() {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    val spec = PipelineModSpec(
        "test", "A test module", listOf(
            ModOp(
                ModOpType.SET_ARGS,
                mapOf("extract_pages" to true),
                OpFilter(OpFilterType.SUBSTR, "OfficeImporter")
            )
        )
    )

    @Test
    fun testCreate() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/pipeline-mods")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(spec.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo(spec.description)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ops[0].type", CoreMatchers.equalTo("SET_ARGS")))
            .andReturn()
    }

    @Test
    fun testUpdate() {

        val op = ModOp(
            ModOpType.APPEND,
            listOf(ProcessorRef("foo", "bar"))
        )

        val mod = pipelineModService.create(spec)
        val update = PipelineModUpdate(name = "cats", description = "dogs",
            restricted = true, ops = listOf(op))

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/pipeline-mods/${mod.id}")
                .headers(admin())
                .content(Json.serialize(update))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("cats")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo("dogs")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.restricted", CoreMatchers.equalTo(true)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ops[0].type", CoreMatchers.equalTo("APPEND")))
            .andReturn()
    }

    @Test
    fun testGet() {
        val mod = pipelineModService.create(spec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/pipeline-mods/${mod.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(spec.name)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.description", CoreMatchers.equalTo(spec.description)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.ops[0].type", CoreMatchers.equalTo("SET_ARGS")))
            .andReturn()
    }

    @Test
    fun testDelete() {
        val mod = pipelineModService.create(spec)
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/pipeline-mods/${mod.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
}
