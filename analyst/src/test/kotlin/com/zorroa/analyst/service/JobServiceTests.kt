package com.zorroa.analyst.service

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.domain.JobSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.web.WebAppConfiguration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@WebAppConfiguration
class JobServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Test
    fun testCreate() {
        val spec = JobSpec(
                "unit-test-task",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t = jobService.create(spec)
        assertNotNull(t.id)
        assertEquals(spec.assetId, t.assetId)
        assertEquals(spec.organizationId, t.organizationId)
        assertEquals(spec.pipelines, t.pipelines)
        assertEquals(spec.name, t.name)
    }
}
