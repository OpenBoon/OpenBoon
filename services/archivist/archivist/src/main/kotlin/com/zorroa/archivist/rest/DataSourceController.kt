package com.zorroa.archivist.rest

import com.zorroa.archivist.clients.ZmlpUser
import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceCredentials
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.service.DataSourceService
import com.zorroa.archivist.util.RestUtils
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
class DataSourceController(
    val dataSourceService: DataSourceService
) {

    @ApiOperation("Create a DataSource")
    @PostMapping("/api/v1/data-sources")
    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'SuperAdmin')")
    fun create(@ApiParam("Create a new data set.") @RequestBody spec: DataSourceSpec): DataSource {
        return dataSourceService.create(spec)
    }

    @ApiOperation("Get a DataSource by id.")
    @GetMapping("/api/v1/data-sources/{id}")
    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'SuperAdmin')")
    fun get(@ApiParam("The DataSource unique Id.") @PathVariable id: UUID): DataSource {
        return dataSourceService.get(id)
    }

    @ApiOperation("Import assets from a DataSource.")
    @PostMapping("/api/v1/data-sources/{id}/_import")
    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'SuperAdmin')")
    fun importAssets(@ApiParam("The DataSource unique Id.") @PathVariable id: UUID): Job {
        val ds = dataSourceService.get(id)
        return dataSourceService.createAnalysisJob(ds)
    }

    @ApiOperation("Update or remove DataSource credentials.")
    @PutMapping("/api/v1/data-sources/{id}/_credentials")
    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'SuperAdmin')")
    fun updateCredentials(
        @ApiParam("The DataSource Id") @PathVariable id: UUID,
        @ApiParam("A credentials blob") @RequestBody creds: DataSourceCredentials
    ): Any {
        if (!dataSourceService.updateCredentials(id, creds.blob)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")
        }
        return RestUtils.updated(LogObject.DATASOURCE, id)
    }

    @ApiOperation("Get DataSource credentials.  Only obtainable by a JobRunner key.", hidden = true)
    @GetMapping("/api/v1/data-sources/{id}/_credentials")
    @PreAuthorize("hasAuthority('JobRunner')")
    fun getCredentials(
        @ApiParam("The DataSource Id") @PathVariable id: UUID
    ): DataSourceCredentials {
        return dataSourceService.getCredentials(id)
    }
}