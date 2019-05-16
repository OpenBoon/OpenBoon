package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.TaskError
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@Timed
class TaskErrorController @Autowired constructor(val jobService: JobService) {

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value= ["/api/v1/taskerrors/_search"])
    fun getAll(@RequestBody filter: TaskErrorFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): KPagedList<TaskError> {
        // Backwards compat
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return jobService.getTaskErrors(filter)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value= ["/api/v1/taskerrors/_findOne"])
    fun findOne(@RequestBody filter: TaskErrorFilter): TaskError {
        return jobService.findOneTaskError(filter)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value= ["/api/v1/taskerrors/{id}"])
    fun delete(@PathVariable id: UUID) : Any {
        return HttpUtils.deleted("TaskError", id, jobService.deleteTaskError(id))
    }
}

