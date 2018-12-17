package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.TaskError
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*


@RestController
class TaskErrorController @Autowired constructor(val jobService: JobService) {

    @PostMapping(value= ["/api/v1/taskerrors/_search"])
    fun getAll(@RequestBody filter: TaskErrorFilter) : KPagedList<TaskError> {
        return jobService.getTaskErrors(filter)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SUPERADMIN)")
    @PostMapping(value= ["/api/v1/taskerrors/{id}"])
    fun delete(@PathVariable id: UUID) : Any {
        return HttpUtils.deleted("TaskError", id, jobService.deleteTaskError(id))
    }
}

