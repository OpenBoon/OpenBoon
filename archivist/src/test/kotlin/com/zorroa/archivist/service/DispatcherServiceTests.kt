package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.common.domain.JobSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DispatcherServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Test
    fun testGetNext() {
        val analyst = "http://10.0.0.1:8080"
        val spec = JobSpec("test_job",
                PipelineType.Import,
                listOf(ZpsScript("foo"), ZpsScript("bar")),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)

        val next = dispatcherService.getNext(analyst)
        assertNotNull(next)
        next?.let {
            assertEquals(job.id, it.jobId)
            val host :String = this.jdbc.queryForObject("SELECT str_endpoint FROM task WHERE pk_task=?",
                    String::class.java, it.id)
            assertEquals(analyst, host)
        }
    }


    @Test
    fun testExpand() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                listOf(ZpsScript("foo"), ZpsScript("bar")),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobService.create(spec)
        val task = dispatcherService.expand(job, ZpsScript("bar"))
        assertEquals(job.id, task.jobId)
    }
}