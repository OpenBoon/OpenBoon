package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.DependSpec
import boonai.archivist.domain.DependState
import boonai.archivist.domain.DependType
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.emptyZpsScripts
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DependServiceTests : AbstractTest() {

    @Autowired
    lateinit var dependService: DependService

    @Autowired
    lateinit var jobService: JobService

    @Test
    fun testCreateAndResolveJobOnJobDepend() {
        val spec = JobSpec(
            null,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job1 = jobService.create(spec)
        val job2 = jobService.create(spec)

        val dspec = DependSpec(
            DependType.JobOnJob,
            job1.id,
            job2.id
        )

        var depend = dependService.createDepend(dspec)

        assertEquals(DependType.JobOnJob, depend.type)
        assertEquals(job1.id, depend.dependErJobId)
        assertEquals(job2.id, depend.dependOnJobId)
        assertNull(depend.dependOnTaskId)
        assertNull(depend.dependErTaskId)

        dependService.resolveDependsOnJob(job2)

        depend = dependService.getDepend(depend.id)
        assertEquals(DependState.Inactive, depend.state)
    }

    @Test
    fun testCreateAndResolveTaskOnTaskDepend() {
        val spec = JobSpec(
            null,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job1 = jobService.create(spec)
        val tasks1 = jobService.getTasks(job1.id)
        val job2 = jobService.create(spec)
        val tasks2 = jobService.getTasks(job2.id)

        val dspec = DependSpec(
            DependType.TaskOnTask,
            job1.id,
            job2.id,
            tasks1[0].id,
            tasks2[0].id
        )

        var depend = dependService.createDepend(dspec)

        assertEquals(DependType.TaskOnTask, depend.type)
        assertEquals(job1.id, depend.dependErJobId)
        assertEquals(job2.id, depend.dependOnJobId)
        assertEquals(tasks1[0].id, depend.dependErTaskId)
        assertEquals(tasks2[0].id, depend.dependOnTaskId)

        dependService.resolveDependsOnJob(tasks2[0])

        depend = dependService.getDepend(depend.id)
        assertEquals(DependState.Inactive, depend.state)
    }

    @Test
    fun testGetDepend() {
        val spec = JobSpec(
            null,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job1 = jobService.create(spec)
        val job2 = jobService.create(spec)

        val dspec = DependSpec(
            DependType.JobOnJob,
            job1.id,
            job2.id
        )

        var depend1 = dependService.createDepend(dspec)
        var depend2 = dependService.getDepend(depend1.id)
        assertEquals(depend1.id, depend2.id)
    }
}
