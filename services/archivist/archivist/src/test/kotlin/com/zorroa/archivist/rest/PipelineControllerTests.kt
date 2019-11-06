package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ZpsSlot
import com.zorroa.archivist.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PipelineControllerTests : MockMvcTest() {

    lateinit var pl: Pipeline
    lateinit var spec: PipelineSpec

    @Before
    fun init() {
        spec = PipelineSpec("Zorroa Test", ZpsSlot.Execute, processors = listOf())
        pl = pipelineService.create(spec)
    }

    @Test
    fun testCreate() {

        val spec = PipelineSpec("ZorroaTest2", ZpsSlot.Execute, processors = listOf())
        val result = mvc.perform(
            post("/api/v1/pipelines")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
        val p = deserialize(result, Pipeline::class.java)
        assertEquals(spec.slot, p.slot)
        assertEquals(spec.processors, p.processors)
        assertEquals(spec.name, p.name)
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {

        mvc.perform(
            delete("/api/v1/pipelines/" + pl.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdate() {
        val spec2 = Pipeline(
            pl.id,
            "Rocky IV",
            ZpsSlot.Execute,
            listOf()
        )

        val result = mvc.perform(
            put("/api/v1/pipelines/" + pl.id)
                .headers(admin())
                .content(Json.serialize(spec2))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val rs = deserialize(result, Json.GENERIC_MAP)
        assertTrue(rs.get("success") as Boolean)
    }

    @Test
    fun testGet() {

        val result = mvc.perform(
            get("/api/v1/pipelines/" + pl.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val data = deserialize(result, Pipeline::class.java)
        assertEquals(pl.id, data.id)
    }

    @Test
    @Throws(Exception::class)
    fun testGetByName() {

        val result = mvc.perform(
            get("/api/v1/pipelines/" + pl.name)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val data = deserialize(result, Pipeline::class.java)
        assertEquals(pl, data)
    }
}
