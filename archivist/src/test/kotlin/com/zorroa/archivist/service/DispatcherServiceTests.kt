package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.common.domain.JobSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DispatcherServiceTests : AbstractTest() {

    @Autowired
    lateinit var taskDao: TaskDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Test
    fun testGetNext() {
        val analyst = "https://127.0.0.1:5000"
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)

        authenticateAsAnalyst()
        val next = dispatcherService.getNext()
        assertNotNull(next)
        next?.let {
            assertEquals(job.id, it.jobId)
            val host :String = this.jdbc.queryForObject("SELECT str_host FROM task WHERE pk_task=?",
                    String::class.java, it.id)
            assertEquals(analyst, host)
        }
    }

    @Test
    fun testExpand() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobService.create(spec)
        val task = dispatcherService.expand(job, emptyZpsScript("bar"))
        assertEquals(job.id, task.jobId)
    }


    @Test
    fun testExpandFromParentTask() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobService.create(spec)
        val zps = emptyZpsScript("bar")
        zps.execute = mutableListOf(ProcessorRef("foo"))

        val task = dispatcherService.expand(job, zps)
        val task2 = dispatcherService.expand(task, emptyZpsScript("bar"))
        val zps2 = taskDao.getScript(task2.id)

        assertNotNull(zps2.execute)
        // Validate task2 inherited from task
        assertEquals(1, zps.execute!!.size)
    }
}