package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.TaskSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class TaskErrorDaoTests : AbstractTest() {

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var jobService: JobService


    @Test
    fun testCreate() {

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        val error = TaskErrorEvent(UUID.randomUUID(), "/foo/bar.jpg",
                "it broke", "com.zorroa.ImageIngestor", true)
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        val result = taskErrorDao.create(event, error)
    }

}