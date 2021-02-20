package boonai.archivist.rest

import boonai.archivist.domain.TaskError
import boonai.archivist.domain.TaskErrorFilter
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.JobService
import boonai.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('DataQueueManage')")
@RestController
@Timed
@Api(tags = ["Task Error"], description = "Operations for interacting with Task Errors.")
class TaskErrorController @Autowired constructor(val jobService: JobService) {

    @ApiOperation("Search for Task Errors.")
    @PostMapping(value = ["/api/v1/taskerrors/_search"])
    fun getAll(
        @ApiParam("Search filter.") @RequestBody filter: TaskErrorFilter,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): KPagedList<TaskError> {
        // Backwards compat
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return jobService.getTaskErrors(filter)
    }

    @ApiOperation(
        "Searches for a single Task Error.",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/taskerrors/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody filter: TaskErrorFilter): TaskError {
        return jobService.findOneTaskError(filter)
    }

    @ApiOperation("Delete a Task Error.")
    @DeleteMapping(value = ["/api/v1/taskerrors/{id}"])
    fun delete(@ApiParam("UUID of the Task Error.") @PathVariable id: UUID): Any {
        return HttpUtils.deleted("TaskError", id, jobService.deleteTaskError(id))
    }
}
