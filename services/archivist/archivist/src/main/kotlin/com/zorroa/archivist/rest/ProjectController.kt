package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.ProjectService
import com.zorroa.archivist.util.HttpUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
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
@Api(tags = ["Project"], description = "Operations for managing Projects.")
class ProjectController constructor(
    val projectService: ProjectService
) {

    @PostMapping(value = ["/api/v1/projects"])
    @ApiOperation("Create Project.")
    fun create(@RequestBody spec: ProjectSpec): Project {
        return projectService.create(spec)
    }

    @GetMapping(value = ["/api/v1/projects/{id}"])
    @ApiOperation("Retrieve Project by Id.")
    fun get(@PathVariable id: UUID): Project {
        return projectService.get(id)
    }

    @DeleteMapping(value = ["/api/v1/projects/{id}"])
    @ApiOperation("Delete Project by Id.")
    fun delete(@PathVariable id: UUID): Any {
        try {
            projectService.delete(id)
            return HttpUtils.deleted("projects", id, true);
        } catch (ex: Exception) {
            return HttpUtils.deleted("projects", id, false);
        }
    }

    @RequestMapping(value = ["/api/v1/projects/_search"], method = [RequestMethod.POST, RequestMethod.GET])
    @ApiOperation("Get all Projects")
    fun getAll(@RequestBody(required = false) filter: ProjectFilter?): KPagedList<Project> {
        return projectService.getAll(filter ?: ProjectFilter())
    }

    @RequestMapping(value = ["/api/v1/projects/_findOne"], method = [RequestMethod.POST, RequestMethod.GET])
    @ApiOperation("Search Filter")
    fun findOne(@RequestBody(required = false) filter: ProjectFilter?): Project {
        return projectService.findOne(filter ?: ProjectFilter())
    }
}