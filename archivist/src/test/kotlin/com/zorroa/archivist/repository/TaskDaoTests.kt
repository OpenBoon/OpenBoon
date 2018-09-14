package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.domain.*
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

    @Autowired
    internal lateinit var taskDao: TaskDao

    internal lateinit var task : Task
    internal lateinit var spec : TaskSpec
    internal lateinit var job : Job

    @Before
    fun init() {
        val jspec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        job = jobDao.create(jspec, PipelineType.Import)
        spec = TaskSpec("generator", jspec.script!!)
        task = taskDao.create(job, spec)
    }

    @Test
    fun testGetScript() {
        val script = taskDao.getScript(task.id)
        assertEquals("test_script", script.name)
    }

    @Test
    fun testGet() {
        val task2 = taskDao.get(task.id)
        assertEquals(task.id, task2.id)
        assertEquals(job.organizationId, task2.organizationId)
    }

    @Test
    fun testCreate() {
        assertEquals(job.id, task.jobId)
        assertEquals(spec.name, task.name)
    }

    @Test
    fun testSetState() {
        assertTrue(taskDao.setState(task, TaskState.Running, null))
        assertFalse(taskDao.setState(task, TaskState.Running, TaskState.Waiting))
        assertFalse(taskDao.setState(task, TaskState.Skip, TaskState.Waiting))
    }
}
