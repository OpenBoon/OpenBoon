package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.TaskError
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.emptyZpsScripts
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.zmlp.util.Json
import com.zorroa.archivist.util.randomString
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

class TaskErrorControllerTests : MockMvcTest() {

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var jobService: JobService

    @Before
    fun init() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            randomString(), "/foo/bar.jpg",
            "it broke", "com.zorroa.OfficeIngestor", true, "execute"
        )
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        taskErrorDao.create(task, error)
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {

        val filter = TaskErrorFilter(processors = listOf("com.zorroa.OfficeIngestor"))

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/taskerrors/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val content = result.response.contentAsString
        val log = Json.Mapper.readValue<KPagedList<TaskError>>(
            content,
            object : TypeReference<KPagedList<TaskError>>() {}
        )
        assertEquals(1, log.size())
    }

    @Test
    fun testFindOne() {
        val taskError = resultForPostContent<TaskError>(
            "/api/v1/taskerrors/_findOne",
            TaskErrorFilter(processors = listOf("com.zorroa.OfficeIngestor"))
        )
        assertEquals("/foo/bar.jpg", taskError.path)
    }
}
