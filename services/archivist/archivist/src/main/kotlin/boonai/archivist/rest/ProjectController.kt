package boonai.archivist.rest

import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexTask
import boonai.archivist.domain.Project
import boonai.archivist.domain.ProjectFilter
import boonai.archivist.domain.ProjectIndexMigrationSpec
import boonai.archivist.domain.ProjectNameUpdate
import boonai.archivist.domain.ProjectQuotas
import boonai.archivist.domain.ProjectQuotasTimeSeriesEntry
import boonai.archivist.domain.ProjectSpec
import boonai.archivist.domain.ProjectTierUpdate
import boonai.archivist.repository.KPagedList
import boonai.archivist.security.getProjectId
import boonai.archivist.service.IndexRoutingService
import boonai.archivist.service.IndexTaskService
import boonai.archivist.service.ProjectService
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import java.util.Date
import java.util.UUID

/**
 * The /api/v1/projects endpoints (plural) are mainly admin endpoints that
 * can see multiple projects.
 *
 * The /api/v1/project endpoints utilize the authed API Key's project.
 *
 */
@RestController
@Api(tags = ["Project"], description = "Operations for managing Projects.")
class ProjectController constructor(
    val projectService: ProjectService,
    val projectStorageService: ProjectStorageService,
    val indexRoutingService: IndexRoutingService,
    val indexTaskService: IndexTaskService
) {

    @PreAuthorize("hasAuthority('SystemManage')")
    @PostMapping(value = ["/api/v1/projects"])
    @ApiOperation("Create Project.")
    fun create(@RequestBody spec: ProjectSpec): Project {
        return projectService.create(spec)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @DeleteMapping(value = ["/api/v1/projects/{id}"])
    @ApiOperation("Delete Project.")
    fun delete(@PathVariable id: UUID) {
        val project = projectService.get(id)
        return projectService.delete(project.id)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @GetMapping(value = ["/api/v1/projects/{id}"])
    @ApiOperation("Retrieve Project by Id.")
    fun get(@PathVariable id: UUID): Project {
        return projectService.get(id)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PostMapping(value = ["/api/v1/projects/{id}/_migrate"])
    @ApiOperation("Migrate a project to a new index.")
    fun migrate(@PathVariable id: UUID, @RequestBody spec: ProjectIndexMigrationSpec): IndexTask {
        return indexTaskService.migrateProject(projectService.get(id), spec)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @GetMapping(value = ["/api/v1/projects/{id}/_index"])
    @ApiOperation("Migrate a project to a new index.")
    fun getIndex(@PathVariable id: UUID): IndexRoute {
        val project = projectService.get(id)
        return indexRoutingService.getIndexRoute(project.indexRouteId as UUID)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/projects/{id}/_index/{index}"])
    @ApiOperation("Set the project's new index.")
    fun setIndex(@PathVariable id: UUID, @PathVariable index: UUID): Any {
        val project = projectService.get(id)
        val index = indexRoutingService.getIndexRoute(index)
        return HttpUtils.status(
            "project", id.toString(),
            "set-index", projectService.setIndexRoute(project, index)
        )
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

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/projects/{id}/_enable"])
    @ApiOperation("Set an disabled project to enabled.")
    fun putEnabled(@PathVariable id: UUID): Any {
        projectService.setEnabled(id, true)
        return HttpUtils.status("project", id.toString(), "enable", true)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/projects/{id}/_disable"])
    @ApiOperation("Set a disabled project to enabled.")
    fun putDisabled(@PathVariable id: UUID): Any {
        projectService.setEnabled(id, false)
        return HttpUtils.status("project", id.toString(), "disable", true)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/projects/{id}/_update_tier"])
    @ApiOperation("Update Project Tier")
    fun updateProjectTier(
        @PathVariable id: UUID,
        @RequestBody(required = true) projectTierUpdate: ProjectTierUpdate
    ): Project {
        projectService.setTier(id, projectTierUpdate.tier)
        return projectService.get(id)
    }

    //
    // Methods that default to the API Keys project Id.
    //
    @GetMapping(value = ["/api/v1/project"])
    @ApiOperation("Retrieve my current project.")
    fun getMyProject(): Project {
        return projectService.get(getProjectId())
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/project/_quotas"])
    @ApiOperation("Retrieve my current project quotas")
    fun getMyProjectQuotas(): ProjectQuotas {
        return projectService.getQuotas(getProjectId())
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/project/_quotas_time_series"])
    @ApiOperation("Retrieve time serious measurements of quota counters")
    fun getMyProjectQuotasTimeSeries(
        @RequestParam("start", required = false) start: Long?,
        @RequestParam("stop", required = false) stop: Long?
    ): List<ProjectQuotasTimeSeriesEntry> {
        return projectService.getQuotasTimeSeries(
            getProjectId(),
            Date(start ?: System.currentTimeMillis() - 86400000L),
            Date(stop ?: System.currentTimeMillis())
        )
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @PutMapping(value = ["/api/v1/project/_rename"])
    @ApiOperation("Rename Project")
    fun renameProject(@RequestBody(required = true) nameUpdate: ProjectNameUpdate): Any {
        val id = getProjectId()
        projectService.rename(id, nameUpdate)
        return HttpUtils.status("project", id.toString(), "rename", true)
    }
}
