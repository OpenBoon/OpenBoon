package boonai.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.AbstractTest
import boonai.archivist.domain.ProcessorFilter
import boonai.archivist.domain.ProcessorSpec
import boonai.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import kotlin.test.assertTrue

class ProcessorServiceTests : AbstractTest() {

    @Autowired
    lateinit var processorService: ProcessorService

    @Test
    fun testReplaceAll() {
        val specs = Json.Mapper.readValue<List<ProcessorSpec>>(
            ClassPathResource("processors.json").inputStream
        )
        processorService.replaceAll(specs)
        // 2nd time should fail unless replacement is happening
        processorService.replaceAll(specs)
        assertTrue(processorService.getAll(ProcessorFilter()).size() > 0)
    }
}
