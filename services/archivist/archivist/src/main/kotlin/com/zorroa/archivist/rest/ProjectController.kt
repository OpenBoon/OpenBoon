package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSettings
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.ProjectService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Api(tags = ["Project"], description = "Operations for managing Projects.")
class ProjectController constructor(
    val projectService: ProjectService
) {

    @PreAuthorize("hasAuthority('SystemManage')")
    @PostMapping(value = ["/api/v1/projects"])
    @ApiOperation("Create Project.")
    fun create(@RequestBody spec: ProjectSpec): Project {
        return projectService.create(spec)
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/projects/{id}"])
    @ApiOperation("Retrieve Project by Id.")
    fun get(@PathVariable id: UUID): Project {
        return projectService.get(id)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @RequestMapping(value = ["/api/v1/projects/_search"], method = [RequestMethod.POST, RequestMethod.GET])
    @ApiOperation("Get all Projects")
    fun getAll(@RequestBody(required = false) filter: ProjectFilter?): KPagedList<Project> {
        return projectService.getAll(filter ?: ProjectFilter())
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @RequestMapping(value = ["/api/v1/projects/_findOne"], method = [RequestMethod.POST, RequestMethod.GET])
    @ApiOperation("Search for a single project")
    fun findOne(@RequestBody(required = false) filter: ProjectFilter?): Project {
        return projectService.findOne(filter ?: ProjectFilter())
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/projects/{id}/_settings"])
    @ApiOperation("Get the project Settings")
    fun getSettings(@PathVariable id: UUID): ProjectSettings {
        return projectService.getSettings(id)
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @PutMapping(value = ["/api/v1/projects/{id}/_settings"])
    @ApiOperation("Get the project Settings")
    fun putSettings(@PathVariable id: UUID, @RequestBody(required = true) settings: ProjectSettings): ProjectSettings {
        projectService.updateSettings(id, settings)
        return projectService.getSettings(id)
    }
}