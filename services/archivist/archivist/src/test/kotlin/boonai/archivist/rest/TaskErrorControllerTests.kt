package boonai.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import boonai.archivist.MockMvcTest
import boonai.archivist.domain.TaskError
import boonai.archivist.domain.TaskErrorEvent
import boonai.archivist.domain.TaskErrorFilter
import boonai.archivist.domain.TaskEvent
import boonai.archivist.domain.TaskEventType
import boonai.archivist.domain.emptyZpsScript
import boonai.archivist.repository.TaskErrorDao
import boonai.archivist.service.JobService
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.emptyZpsScripts
import boonai.archivist.repository.KPagedList
import boonai.common.util.Json
import boonai.archivist.util.randomString
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
        val task = jobService.createTask(job, emptyZpsScript("bar"))

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
