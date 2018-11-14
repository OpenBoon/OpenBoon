package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.emptyZpsScript
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
    fun getPagedByFilter() {
        for (i in 0..9) {
            taskDao.create(job, TaskSpec("test$i", emptyZpsScript("test_script")))
        }

        val filter1 = TaskFilter(jobIds=listOf(job.id))
        assertEquals(10, taskDao.getAll(Pager.first(), filter1).size())

        val filter2 = TaskFilter(jobIds=listOf(UUID.randomUUID()))
        assertEquals(0, taskDao.getAll(Pager.first(), filter2).size())

        val filter3 = TaskFilter(jobIds=listOf(job.id))
        assertEquals(1, taskDao.getAll(Pager.first(1), filter3).size())
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
        assertEquals(1, taskStateCount(job, TaskState.Waiting))
        assertTrue(taskDao.setState(task, TaskState.Running, null))
        assertEquals(0, taskStateCount(job, TaskState.Waiting))
        assertEquals(1, taskStateCount(job, TaskState.Running))
        assertFalse(taskDao.setState(task, TaskState.Running, TaskState.Waiting))
        assertFalse(taskDao.setState(task, TaskState.Skipped, TaskState.Waiting))
    }

    fun taskStateCount(job: JobId, state:TaskState) : Int {
        val ord = state.ordinal
        return jdbc.queryForObject("SELECT int_task_state_$ord FROM job_count WHERE pk_job=?", Int::class.java,job.jobId);
    }

    @Test
    fun testIncrementAssetStats() {
        val counts = BatchCreateAssetsResponse(6)
        counts.createdAssetIds.add("foo")
        counts.replacedAssetIds.addAll(listOf("foo", "bar"))
        counts.erroredAssetIds.addAll(listOf("foo", "bar", "bing"))
        counts.warningAssetIds.addAll(listOf("foo", "bar", "bing", "bang"))
        assertTrue(taskDao.incrementAssetStats(task, counts))

        val map = jdbc.queryForMap("SELECT * FROM task_stat WHERE pk_task=?", task.id)
        print(map)
        assertEquals(counts.createdAssetIds.size, map["int_asset_create_count"])
        assertEquals(counts.replacedAssetIds.size, map["int_asset_replace_count"])
        assertEquals(counts.erroredAssetIds.size, map["int_asset_error_count"])
        assertEquals(counts.warningAssetIds.size, map["int_asset_warning_count"])
        assertEquals(counts.total, map["int_asset_total_count"])
    }
}
