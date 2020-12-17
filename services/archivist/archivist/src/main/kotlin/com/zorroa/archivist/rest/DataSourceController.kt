package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceDelete
import com.zorroa.archivist.domain.DataSourceFilter
import com.zorroa.archivist.domain.DataSourceImportOptions
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.repository.DataSourceJdbcDao
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.CredentialsService
import com.zorroa.archivist.service.DataSourceService
import com.zorroa.archivist.service.JobLaunchService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('DataSourceManage')")
@RestController
@Timed
class DataSourceController(
    val dataSourceService: DataSourceService,
    val dataSourceJdbcDao: DataSourceJdbcDao,
    val credentialsService: CredentialsService,
    val jobLaunchService: JobLaunchService
) {

    @ApiOperation("Create a DataSource")
    @PostMapping("/api/v1/data-sources")
    fun create(@ApiParam("Create a new data set.") @RequestBody spec: DataSourceSpec): DataSource {
        return dataSourceService.create(spec)
    }

    @PutMapping("/api/v1/data-sources/{id}")
    fun update(
        @ApiParam("The DataSource unique Id.") @PathVariable id: UUID,
        @ApiParam("Create a new data set.") @RequestBody update: DataSourceUpdate
    ): DataSource {
        return dataSourceService.update(id, update)
    }

    @DeleteMapping("/api/v1/data-sources/{id}")
    fun delete(
        @ApiParam("The DataSource unique Id.") @PathVariable id: UUID,
        @RequestBody(required = false) dataSourceDelete: DataSourceDelete?
    ): Any {
        dataSourceService.delete(
            id,
            dataSourceDelete ?: DataSourceDelete()
        )
        return HttpUtils.deleted("DataSource", id, true)
    }

    @ApiOperation("Get a DataSource by id.")
    @GetMapping("/api/v1/data-sources/{id}")
    fun get(@ApiParam("The DataSource unique Id.") @PathVariable id: UUID): DataSource {
        return dataSourceService.get(id)
    }

    @ApiOperation("Search for DataSources.")
    @PostMapping("/api/v1/data-sources/_search")
    fun find(@RequestBody(required = false) filter: DataSourceFilter?): KPagedList<DataSource> {
        return dataSourceJdbcDao.find(filter ?: DataSourceFilter())
    }

    @ApiOperation("Find a single DataSource")
    @PostMapping("/api/v1/data-sources/_findOne")
    fun findOneV1(@RequestBody(required = false) filter: DataSourceFilter?): DataSource {
        return dataSourceJdbcDao.findOne(filter ?: DataSourceFilter())
    }

    @ApiOperation("Find a single DataSource")
    @PostMapping("/api/v1/data-sources/_find_one")
    fun findOneV3(@RequestBody(required = false) filter: DataSourceFilter?): DataSource {
        return dataSourceJdbcDao.findOne(filter ?: DataSourceFilter())
    }

    @ApiOperation("Import assets from a DataSource.")
    @PostMapping("/api/v1/data-sources/{id}/_import")
    fun importAssets(
        @ApiParam("The DataSource unique Id.") @PathVariable id: UUID,
        @RequestBody(required = false) options: DataSourceImportOptions?
    ): Job {
        return jobLaunchService.launchJob(
            dataSourceService.get(id),
            options ?: DataSourceImportOptions()
        )
    }
}
