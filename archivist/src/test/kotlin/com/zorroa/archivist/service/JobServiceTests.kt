package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.common.domain.JobSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class JobServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Test
    fun testCreate() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                listOf(ZpsScript("foo"), ZpsScript("bar")),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobService.create(spec)
        assertEquals(spec.name, job.name)
        assertEquals(spec.type, job.type)
    }
}