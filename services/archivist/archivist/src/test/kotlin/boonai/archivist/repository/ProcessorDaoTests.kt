package boonai.archivist.repository

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.AbstractTest
import boonai.archivist.domain.ProcessorFilter
import boonai.archivist.domain.ProcessorSpec
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessorDaoTests : AbstractTest() {

    @Autowired
    lateinit var processorDao: ProcessorDao

    @Test
    fun testBatchCreate() {
        val specs = Json.Mapper.readValue<List<ProcessorSpec>>(
            ClassPathResource("processors.json").inputStream
        )
        assertTrue(processorDao.batchCreate(specs) > 0)
    }

    @Test
    fun testDeleteAll() {
        val specs = Json.Mapper.readValue<List<ProcessorSpec>>(
            ClassPathResource("processors.json").inputStream
        )
        assertTrue(processorDao.batchCreate(specs) > 0)
        processorDao.deleteAll()
        assertEquals(processorDao.getAll(ProcessorFilter()).size(), 0)
    }

    @Test
    fun testGetAll() {
        val specs = Json.Mapper.readValue<List<ProcessorSpec>>(
            ClassPathResource("processors.json").inputStream
        )
        processorDao.batchCreate(specs)

        val procs = processorDao.getAll(ProcessorFilter())
        println(procs.list[0].id)
        println(procs.list[1].id)
        assertTrue(procs.size() > 0)
    }

    @Test
    fun testGetAllWithFilter() {
        val specs = Json.Mapper.readValue<List<ProcessorSpec>>(
            ClassPathResource("processors.json").inputStream
        )
        processorDao.batchCreate(specs)

        // Class names
        var filter = ProcessorFilter(classNames = listOf("zplugins.core.generators.AssetSearchGenerator"))
        var procs = processorDao.getAll(filter)
        assertEquals(procs.size(), 1)
        // types names
        filter = ProcessorFilter(types = listOf("generate"))
        procs = processorDao.getAll(filter)
        assertEquals(procs.size(), 9)

        // file types names
        filter = ProcessorFilter(fileTypes = listOf("jpg"))
        procs = processorDao.getAll(filter)
        assertEquals(procs.size(), 2)

        // keywords
        filter = ProcessorFilter(keywords = "ingestor")
        procs = processorDao.getAll(filter)
        assertEquals(procs.size(), 4)

        // keywords
        filter = ProcessorFilter(keywords = "google")
        procs = processorDao.getAll(filter)
        assertEquals(procs.size(), 7)

        // ids
        filter = ProcessorFilter(
            ids = listOf(
                UUID.fromString("eebf2132-4b50-5eb0-a240-debfeaea2c6f"),
                UUID.fromString("8bd78f42-ef43-506e-99c0-d65db28e92f7")
            )
        )
        procs = processorDao.getAll(filter)
        assertEquals(procs.size(), 2)
    }
}
