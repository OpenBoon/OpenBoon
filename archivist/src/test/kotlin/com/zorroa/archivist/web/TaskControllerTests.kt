package com.zorroa.archivist.web

import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.Task
import com.zorroa.common.domain.TaskSpec
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

@WebAppConfiguration
class TaskControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    lateinit var task: Task

    @Before
    fun init() {
        val job = launchJob()
        task = jobService.createTask(job, TaskSpec("bar", emptyZpsScript("bar")))
    }

    fun launchJob() : Job {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        return jobService.create(spec)
    }

    @Test
    fun testGet() {
        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.get("/api/v1/tasks/" + task.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val t1 = deserialize(result, Task::class.java)
        assertEquals(task.id, t1.id)
    }

}