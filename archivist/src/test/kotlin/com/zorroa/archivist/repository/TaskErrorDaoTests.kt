package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.common.domain.TaskErrorEvent
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class TaskErrorDaoTests : AbstractTest() {

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Test
    fun testCreate() {
        val error = TaskErrorEvent(UUID.randomUUID(), "/foo/bar.jpg",
                "it broke", "com.zorroa.ImageIngestor", true)
        val event = TaskEvent("error","https://localhost:8080",
                UUID.randomUUID(), UUID.randomUUID(), error)

        val result = taskErrorDao.create(event, error)

    }

}