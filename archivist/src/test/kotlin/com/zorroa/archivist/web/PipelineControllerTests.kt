package com.zorroa.archivist.web

import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.service.PipelineService
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineControllerTests : MockMvcTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    lateinit var pl: Pipeline
    lateinit var spec: PipelineSpec

    @Before
    fun init() {
        spec = PipelineSpec("Zorroa Test", PipelineType.Import, "test", processors=listOf())
        pl = pipelineService.create(spec)
    }

    @Test
    fun testCreate() {
        val session = admin()

        val spec = PipelineSpec("Zorroa Test2", PipelineType.Import, "test", processors=listOf())
        val result = mvc.perform(post("/api/v1/pipelines")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()
        val p = deserialize(result, Pipeline::class.java)
        assertEquals(spec.type, p.type)
        assertEquals(spec.processors, p.processors)
        assertEquals(spec.name, p.name)
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {
        val session = admin()
        val result2 = mvc.perform(delete("/api/v1/pipelines/" + pl.id)
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val rs = deserialize(result2, Json.GENERIC_MAP)
        println(rs)

    }

    @Test
    @Throws(Exception::class)
    fun testUpdate() {
        val spec2 = Pipeline(
                pl.id,
                "Rocky IV",
                PipelineType.Batch,
                listOf())

        val session = admin()
        val result = mvc.perform(put("/api/v1/pipelines/" + pl.id)
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec2))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val rs = deserialize(result, Json.GENERIC_MAP)
        assertTrue(rs.get("success") as Boolean)
    }

    @Test
    fun testGet() {
        val session = admin()
        val result = mvc.perform(get("/api/v1/pipelines/" + pl.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val data = deserialize(result, Pipeline::class.java)
        assertEquals(pl.id, data.id)
    }


    @Test
    @Throws(Exception::class)
    fun testGetByName() {
        val session = admin()
        val result = mvc.perform(get("/api/v1/pipelines/" + pl.name)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val data = deserialize(result, Pipeline::class.java)
        assertEquals(pl, data)
    }
}
