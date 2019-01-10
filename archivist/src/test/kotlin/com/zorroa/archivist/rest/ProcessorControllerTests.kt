package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.Processor
import com.zorroa.archivist.domain.ProcessorFilter
import com.zorroa.archivist.domain.ProcessorSpec
import com.zorroa.archivist.service.ProcessorService
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessorControllerTests : MockMvcTest() {

    @Autowired
    lateinit var processorService: ProcessorService

    @Before
    fun init() {
        val specs = Json.Mapper.readValue<List<ProcessorSpec>>(
                ClassPathResource("processors.json").inputStream)
        processorService.replaceAll(specs)
    }

    @Test
    fun testGetById() {
        val session = admin()
        val id = "642462df-8c96-5688-8f1d-c13ac327832c"
        val result = mvc.perform(MockMvcRequestBuilders.get("/api/v1/processors/$id")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val proc = deserialize(result, Processor::class.java)
        assertEquals(id, proc.id.toString())
    }

    @Test
    fun testGetByClassName() {
        val session = admin()
        val name = "zplugins.duplicates.processors.DuplicateDetectionProcessor"
        val result = mvc.perform(MockMvcRequestBuilders.get("/api/v1/processors/$name")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val proc = deserialize(result, Processor::class.java)
        assertEquals(name, proc.className)
    }

    @Test
    fun testSearch() {
        val session = admin()
        val filter = ProcessorFilter(keywords = "ingestor")

        val result = mvc.perform(MockMvcRequestBuilders.post("/api/v1/processors/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val procs = deserialize(result, object: TypeReference<KPagedList<Processor>>() {})
        assertEquals(4, procs.size())
        procs.forEach {
            assertTrue(it.className.contains("ingestor", ignoreCase = true))
        }
    }

}