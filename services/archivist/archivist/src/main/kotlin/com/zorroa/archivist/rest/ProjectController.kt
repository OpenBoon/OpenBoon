package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.ProjectService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('SuperAdmin')")
@RestController
class ProjectController constructor(
    val projectService: ProjectService
) {

    @PostMapping(value = ["/api/v1/projects"])
    fun create(@RequestBody spec: ProjectSpec) : Project {
        return projectService.create(spec)
    }

    @GetMapping(value = ["/api/v1/projects/{id}"])
    fun get(@PathVariable id: UUID) : Project {
        return projectService.get(id)
    }

    @RequestMapping(value = ["/api/v1/projects/_search"], method=[RequestMethod.POST, RequestMethod.GET])
    fun getAll(@RequestBody(required = false) filter: ProjectFilter?) : KPagedList<Project> {
        return projectService.getAll(filter ?: ProjectFilter())
    }

    @RequestMapping(value = ["/api/v1/projects/_findOne"], method=[RequestMethod.POST, RequestMethod.GET])
    fun findOne(@RequestBody(required = false) filter: ProjectFilter?) : Project {
        return projectService.findOne(filter ?: ProjectFilter())
    }
}