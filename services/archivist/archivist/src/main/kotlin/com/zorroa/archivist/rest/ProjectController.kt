package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageRequest
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSettings
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.ProjectService
import com.zorroa.archivist.storage.ProjectStorageService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@Api(tags = ["Project"], description = "Operations for managing Projects.")
class ProjectController constructor(
    val projectService: ProjectService,
    val projectStorageService: ProjectStorageService
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

    // Methods that default to the API Keys project Id.

    @ApiOperation("Upload a file into project cloud storage.")
    @PreAuthorize("hasAuthority('ProjectFilesWrite')")
    @PostMapping(value = ["/api/v3/project/files"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun uploadFile(
        @RequestPart(value = "file") file: MultipartFile,
        @RequestPart(value = "body") req: ProjectStorageRequest
    ): Any {

        req.entity?.let {
            val locator = ProjectFileLocator(it, req.category, req.name)
            val spec = ProjectStorageSpec(locator, req.attrs, file.bytes)
            return projectStorageService.store(spec)
        }

        throw IllegalArgumentException("An storage entity must be defined.")
    }

    @ApiOperation("Fetch a file from project cloud storage.")
    @PreAuthorize("hasAuthority('ProjectFilesRead')")
    @GetMapping(value = ["/api/v3/project/files/{entity}/{category}/{name}"])
    @ResponseBody
    fun streamFile(
        @PathVariable entity: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): ResponseEntity<Resource> {
        val locator = ProjectFileLocator(
            ProjectStorageEntity.valueOf(entity.toUpperCase()),category, name)
        return projectStorageService.stream(locator)
    }
}