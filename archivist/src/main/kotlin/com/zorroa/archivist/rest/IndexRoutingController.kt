package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.IndexMappingVersion
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteFilter
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.IndexMigrationService
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.service.OrganizationService
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SUPERADMIN)")
@RestController
@Timed
class IndexRoutingController @Autowired constructor(
    val indexRoutingService: IndexRoutingService,
    val indexMigrationService: IndexMigrationService,
    val organizationService: OrganizationService
) {

    @PostMapping(value = ["/api/v1/index-routes"])
    fun create(@RequestBody spec: IndexRouteSpec): IndexRoute {
        return indexRoutingService.createIndexRoute(spec)
    }

    @GetMapping(value = ["/api/v1/index-routes/{id}"])
    fun get(@PathVariable id: UUID): IndexRoute {
        return indexRoutingService.getIndexRoute(id)
    }

    @RequestMapping(value = ["/api/v1/index-routes/_search"], method = [RequestMethod.GET, RequestMethod.POST])
    fun search(@RequestBody(required = false) filter: IndexRouteFilter?): KPagedList<IndexRoute> {
        return indexRoutingService.getAll(filter ?: IndexRouteFilter())
    }

    @RequestMapping(value = ["/api/v1/index-routes/_findOne"], method = [RequestMethod.GET, RequestMethod.POST])
    fun findOne(@RequestBody filter: IndexRouteFilter): IndexRoute {
        return indexRoutingService.findOne(filter)
    }

    @GetMapping(value = ["/api/v1/index-routes/_mappings"])
    fun getMappings(): List<IndexMappingVersion> {
        return indexRoutingService.getIndexMappingVersions()
    }

    @PostMapping(value = ["/api/v1/index-routes/_migrate"])
    fun migrate(@RequestBody mig: IndexMigrationSpec): Any {
        return indexMigrationService.migrate(organizationService.get(getOrgId()), mig)
    }
}