package com.zorroa.irm.studio.service

import com.zorroa.irm.studio.AbstractTest
import com.zorroa.irm.studio.domain.Job
import com.zorroa.irm.studio.domain.JobSpec
import com.zorroa.irm.studio.domain.JobState
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.web.WebAppConfiguration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@WebAppConfiguration
class TaskServiceTests : AbstractTest() {

    @Autowired
    lateinit var taskService: JobService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Test
    fun testCreate() {
        val spec = JobSpec(
                "unit-test-task",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t = taskService.create(spec)
        assertNotNull(t.id)
        assertEquals(spec.assetId, t.assetId)
        assertEquals(spec.organizationId, t.organizationId)
        assertEquals(spec.pipelines, t.pipelines)
        assertEquals(spec.name, t.name)
    }

    @Test
    fun testLaunch() {
        val task = Job(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "unit-test-task",
                JobState.WAITING,
                listOf("test"))
        val job = taskService.start(task)
        assertNotNull(job)
    }
}
