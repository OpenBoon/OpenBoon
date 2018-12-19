package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.util.copyInputToOuput
import com.zorroa.common.domain.TaskFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse


@RestController
class TaskController @Autowired constructor(
        val jobService: JobService,
        val dispatcherService: DispatcherService,
        val taskDao: TaskDao
) {

    @PostMapping(value = ["/api/v1/tasks/_search"])
    @Throws(IOException::class)
    fun search(@RequestBody filter: TaskFilter,
                 @RequestParam(value = "from", required = false) from: Int?,
                 @RequestParam(value = "count", required = false) count: Int?): Any {
        return taskDao.getAll(filter)
    }

    @GetMapping(value = ["/api/v1/tasks/{id}"])
    @Throws(IOException::class)
    fun getTask(@PathVariable id: UUID): Any {
        return taskDao.get(id)
    }

    @PutMapping(value = ["/api/v1/tasks/{id}/_retry"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun retry(@PathVariable id: UUID) : Any {
        return HttpUtils.status("Task", id, "retry",
                dispatcherService.retryTask(jobService.getTask(id)))

    }

    @PutMapping(value = ["/api/v1/tasks/{id}/_skip"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun skip(@PathVariable id: UUID) : Any {

        return HttpUtils.status("Task", id, "skip",
                dispatcherService.skipTask(jobService.getTask(id)))
    }

    @GetMapping(value = ["/api/v1/tasks/{id}/_script"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun getScript(@PathVariable id: UUID): ZpsScript {
        return jobService.getZpsScript(id)
    }

    @GetMapping(value = ["/api/v1/tasks/{id}/_log"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun getLog(@PathVariable id: UUID, rsp: HttpServletResponse) {
        val sf = jobService.getTaskLog(id)
        if (sf.exists()) {
            rsp.contentType = "text/plain"
            rsp.setContentLengthLong(sf.getStat().size)
            copyInputToOuput(sf.getInputStream(), rsp.outputStream)
        }
        else {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @RequestMapping(value = ["/api/v1/tasks/{id}/taskerrors"], method=[RequestMethod.GET, RequestMethod.POST])
    fun getTaskErrors(@PathVariable id: UUID, @RequestBody(required = false) filter: TaskErrorFilter?): Any {
        val fixedFilter = if (filter == null) {
            TaskErrorFilter(taskIds=listOf(id))
        }
        else {
            filter.taskIds = listOf(id)
            filter
        }
        return jobService.getTaskErrors(fixedFilter)
    }

}

