package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.Job
import boonai.archivist.domain.JobPriority
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.TaskExpandEvent
import boonai.archivist.domain.emptyZpsScripts
import boonai.archivist.security.getProjectId
import boonai.archivist.service.DispatcherService
import boonai.archivist.service.JobService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchTaskDaoTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var dispatchTaskDao: DispatchTaskDao

    @Test
    fun testScriptGlobalArgsAreSet() {
        // args from ztool get merged into script.
        launchJob(JobPriority.Standard)
        val tasks = dispatchTaskDao.getNextByJobPriority(JobPriority.Standard, 5)
        assertTrue(tasks.isNotEmpty())
        tasks.forEach {
            assertEquals(it.script.globalArgs!!["captain"], "kirk")
        }
    }

    @Test
    fun testGetByJobPriority() {
        launchJob(JobPriority.Standard)
        val job2 = launchJob(JobPriority.Interactive)
        val job3 = launchJob(JobPriority.Reindex)

        var tasks = dispatchTaskDao.getNextByJobPriority(JobPriority.Interactive, 5)

        assertEquals(2, tasks.size)
        assertEquals(job3.id, tasks[0].jobId)
        assertEquals(job2.id, tasks[1].jobId)
    }

    @Test
    fun testGetNextByOrgSortedByJobPriority() {
        val job1 = launchJob(JobPriority.Standard)
        val job2 = launchJob(JobPriority.Interactive)
        val job3 = launchJob(JobPriority.Reindex)

        var tasks = dispatchTaskDao.getNextByProject(getProjectId(), 5)

        assertEquals(job3.id, tasks[0].jobId)
        assertEquals(job2.id, tasks[1].jobId)
        assertEquals(job1.id, tasks[2].jobId)
    }

    @Test
    fun testGetNextByOrgNextSortedByTime() {
        val job1 = launchJob(JobPriority.Standard)
        val job2 = launchJob(JobPriority.Standard)
        val job3 = launchJob(JobPriority.Standard)

        var tasks = dispatchTaskDao.getNextByProject(getProjectId(), 5)

        assertEquals(job1.id, tasks[0].jobId)
        assertEquals(job2.id, tasks[1].jobId)
        assertEquals(job3.id, tasks[2].jobId)

        dispatcherService.expand(tasks[0], TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg"))))
        Thread.sleep(2)
        dispatcherService.expand(tasks[1], TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg"))))
        Thread.sleep(2)
        dispatcherService.expand(tasks[2], TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg"))))

        tasks = dispatchTaskDao.getNextByProject(getProjectId(), 6)

        // Job that was launched first goes first.
        assertEquals(job1.id, tasks[0].jobId)
        assertEquals(job1.id, tasks[1].jobId)
        assertEquals(job2.id, tasks[2].jobId)
        assertEquals(job2.id, tasks[3].jobId)
        assertEquals(job3.id, tasks[4].jobId)
        assertEquals(job3.id, tasks[5].jobId)
    }

    @Test
    fun testGetNextByOrg() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec)
        val tasks = dispatchTaskDao.getNextByProject(getProjectId(), 5)
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].args.containsKey("foo"))
        assertEquals(spec.env, tasks[0].env)
    }

    @Test
    fun testGetTaskPriority() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        assertTrue(dispatchTaskDao.getDispatchPriority().isEmpty())

        jobService.create(spec)

        val priority = dispatchTaskDao.getDispatchPriority()[0]
        assertEquals(getProjectId(), priority.projectId)
        assertEquals(0, priority.priority)
    }

    @Test
    fun testPeddingTasksStats() {
        var countPendingTasks = dispatchTaskDao.getPendingTasksStats()

        assertEquals(0, countPendingTasks.pendingTasks)
        assertEquals(0, countPendingTasks.maxRunningTasks)

        val launchJob1 = launchJob(JobPriority.Standard)
        val launchJob2 = launchJob(JobPriority.Standard)
        val launchJob3 = launchJob(JobPriority.Standard)
        val maxRunningTasksSum =
            launchJob1.maxRunningTasks + launchJob2.maxRunningTasks + launchJob3.maxRunningTasks + 0L

        countPendingTasks = dispatchTaskDao.getPendingTasksStats()
        assertEquals(3, countPendingTasks.pendingTasks)
        assertEquals(0, countPendingTasks.runningTasks)
        assertEquals(maxRunningTasksSum, countPendingTasks.maxRunningTasks)
    }

    fun launchJob(priority: Int): Job {
        val spec1 = JobSpec(
            "test_job_p$priority",
            emptyZpsScripts("priority_$priority"),
            args = mutableMapOf("captain" to "kirk"),
            priority = priority
        )
        return jobService.create(spec1)
    }
}
