package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.JobSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchTaskDaoTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatchTaskDao: DispatchTaskDao

    @Test
    fun testGetNext() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        jobService.create(spec)
        val tasks = dispatchTaskDao.getNext(getOrgId(), 5)
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].args.containsKey("foo"))
        assertEquals(spec.env, tasks[0].env)
    }

    @Test
    fun testGetTaskPriority() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        assertTrue(dispatchTaskDao.getDispatchPriority().isEmpty())

        jobService.create(spec)

        val priority = dispatchTaskDao.getDispatchPriority()[0]
        assertEquals(getOrgId(), priority.organizationId)
        assertEquals(0, priority.priority)
    }
}