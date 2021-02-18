package boonai.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.MockMvcTest
import boonai.archivist.domain.Processor
import boonai.archivist.domain.ProcessorFilter
import boonai.archivist.domain.ProcessorSpec
import boonai.archivist.service.ProcessorService
import boonai.archivist.repository.KPagedList
import boonai.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
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
            ClassPathResource("processors.json").inputStream
        )
        processorService.replaceAll(testSpecs)
    }

    @Test
    fun testGetById() {

        val id = "eebf2132-4b50-5eb0-a240-debfeaea2c6f"
        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/processors/$id")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val proc = deserialize(result, Processor::class.java)
        assertEquals(id, proc.id.toString())
    }

    @Test
    fun testGetByClassName() {

        val name = "zplugins.duplicates.processors.DuplicateDetectionProcessor"
        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/processors/$name")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val proc = deserialize(result, Processor::class.java)
        assertEquals(name, proc.className)
    }

    @Test
    fun testSearch() {

        val filter = ProcessorFilter(keywords = "ingestor")

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/processors/_search")
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
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
