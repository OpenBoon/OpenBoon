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

    lateinit var testSpecs: List<ProcessorSpec>

    @Before
    fun init() {
        testSpecs = Json.Mapper.readValue<List<ProcessorSpec>>(
                ClassPathResource("processors.json").inputStream)
        processorService.replaceAll(testSpecs)
    }

    @Test
    fun testGetById() {
        val session = admin()
        val id = "eebf2132-4b50-5eb0-a240-debfeaea2c6f"
        val result = mvc.perform(MockMvcRequestBuilders.get("/api/v1/processors/$id")
                .headers(admin())
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
                .headers(admin())
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
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val procs = deserialize(result, object : TypeReference<KPagedList<Processor>>() {})
        assertEquals(4, procs.size())
        procs.forEach {
            assertTrue(it.className.contains("ingestor", ignoreCase = true))
        }
    }

    @Test
    fun findOneTest() {
        val processor = resultForPostContent<Processor>(
            "/api/v1/processors/_findOne",
            ProcessorFilter(classNames = listOf(testSpecs[0].className))
        )
        assertEquals(testSpecs[0].file, processor.file)
    }

    @Test
    fun testFindOneFailsWhenMultipleFound() {
        assertClientErrorForPostContent(
            "/api/v1/processors/_findOne",
            ProcessorFilter(classNames = testSpecs.map { it.className })
        )
    }
}
