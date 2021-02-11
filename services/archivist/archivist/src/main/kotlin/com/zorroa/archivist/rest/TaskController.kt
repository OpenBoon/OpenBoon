package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.domain.TaskFilter
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.service.DependService
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ExecutionException

@PreAuthorize("hasAuthority('DataQueueManage')")
@RestController
@Timed
@Api(tags = ["Task"], description = "Operations for interacting with Tasks.")
class TaskController @Autowired constructor(
    val jobService: JobService,
    val dispatcherService: DispatcherService,
    val dependService: DependService,
    val projectStorageService: ProjectStorageService,
    val taskDao: TaskDao
) {

    @ApiOperation("Search for Tasks.")
    @PostMapping(value = ["/api/v1/tasks/_search"])
    @Throws(IOException::class)
    fun search(
        @ApiParam("Search filter.") @RequestBody filter: TaskFilter,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): Any {
        return taskDao.getAll(filter)
    }

    @ApiOperation(
        "Searches for a single Task.",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/tasks/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody filter: TaskFilter): Task {
        return taskDao.findOne(filter)
    }

    @ApiOperation("Get a Task.")
    @GetMapping(value = ["/api/v1/tasks/{id}"])
    @Throws(IOException::class)
    fun getTask(@ApiParam("UUID of the Task.") @PathVariable id: UUID): Any {
        return taskDao.get(id)
    }

    @ApiOperation("Retry a Task.")
    @PutMapping(value = ["/api/v1/tasks/{id}/_retry"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun retry(@ApiParam("UUID of the Task.") @PathVariable id: UUID): Any {
        val task = jobService.getInternalTask(id)
        val result = dispatcherService.retryTask(
            task,
            "Retried by ${getZmlpActor()}"
        )
        if (result) {
            jobService.restartJob(task)
        }
        return HttpUtils.status("Task", id, "retry", result)
    }

    @ApiOperation("Skip a Task.")
    @PutMapping(value = ["/api/v1/tasks/{id}/_skip"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun skip(@ApiParam("UUID of the Task.") @PathVariable id: UUID): Any {
        return HttpUtils.status(
            "Task", id, "skip",
            dispatcherService.skipTask(jobService.getInternalTask(id))
        )
    }

    @ApiOperation("Get the pipeline script the Task will run.")
    @GetMapping(value = ["/api/v1/tasks/{id}/_script"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun getScript(@ApiParam("UUID of the Task.") @PathVariable id: UUID): ZpsScript {
        return jobService.getZpsScript(id)
    }

    @ApiOperation("Get the task log")
    @GetMapping(value = ["/api/v1/tasks/{id}/_log"])
    @ResponseBody
    @Throws(ExecutionException::class, IOException::class)
    fun streamLog(@ApiParam("UUID of the Task.") @PathVariable id: UUID): ResponseEntity<Resource> {
        val task = jobService.getTask(id)
        return projectStorageService.stream(task.getLogFileLocation())
    }

    @ApiOperation("Get a list of the Task's errors.")
    @RequestMapping(value = ["/api/v1/tasks/{id}/taskerrors"], method = [RequestMethod.GET, RequestMethod.POST])
    fun getTaskErrors(
        @ApiParam("UUID of the Task.") @PathVariable id: UUID,
        @ApiParam("Search filter.") @RequestBody(required = false) filter: TaskErrorFilter?
    ): Any {
        val fixedFilter = if (filter == null) {
            TaskErrorFilter(taskIds = listOf(id))
        } else {
            filter.taskIds = listOf(id)
            filter
        }
        return jobService.getTaskErrors(fixedFilter)
    }

    @ApiOperation("Drop TaskOnTask dependencies")
    @PostMapping(value = ["/api/v1/tasks/{id}/_drop_depends"])
    fun dropDropDependencies(
        @ApiParam("UUID of the Task") @PathVariable id: UUID
    ): Any {
        val task = jobService.getTask(id)
        return mapOf("dropped" to dependService.dropDepends(task))
    }
}
