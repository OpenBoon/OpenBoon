package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Pager
import com.zorroa.common.domain.TaskFilter
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.service.JobService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*


@RestController
class TaskController @Autowired constructor(
        val jobService: JobService,
        val taskDao: TaskDao
) {

    @PostMapping(value = ["/api/v1/tasks/_search"])
    @Throws(IOException::class)
    fun getTasks(@RequestBody filter: TaskFilter,
                 @RequestParam(value = "from", required = false) from: Int?,
                 @RequestParam(value = "count", required = false) count: Int?): Any {
        return taskDao.getAll(Pager(from, count), filter)
    }

    @GetMapping(value = ["/api/v1/tasks/{id}"])
    @Throws(IOException::class)
    fun getTask(@PathVariable id: UUID): Any {
        return taskDao.get(id)
    }
}

