package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.IndexMappingVersion
import com.zorroa.archivist.domain.IndexMigrationSpec
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteFilter
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.service.IndexMigrationService
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
    val indexMigrationService: IndexMigrationService
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
        return indexMigrationService.migrate(mig)
    }

    @PutMapping(value = ["/api/v1/index-routes/{id}/_close"])
    fun close(@PathVariable id: UUID): Any {
        val route = indexRoutingService.getIndexRoute(id)
        val closed = indexRoutingService.closeIndex(route)
        return HttpUtils.updated("index-route", route.id, closed, indexRoutingService.getIndexRoute(id))
    }

    @DeleteMapping(value = ["/api/v1/index-routes/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        val route = indexRoutingService.getIndexRoute(id)
        val deleted = indexRoutingService.deleteIndex(route)
        return HttpUtils.deleted("index-route", route.id, deleted)
    }

    @PutMapping(value = ["/api/v1/index-routes/{id}/_open"])
    fun open(@PathVariable id: UUID): Any {
        val route = indexRoutingService.getIndexRoute(id)
        val closed = indexRoutingService.openIndex(route)
        return HttpUtils.updated("index-route", route.id, closed, indexRoutingService.getIndexRoute(id))
    }

    @GetMapping(value = ["/api/v1/index-routes/{id}/_state"])
    fun getState(@PathVariable id: UUID): Map<String, Any> {
        val route = indexRoutingService.getIndexRoute(id)
        return indexRoutingService.getEsIndexState(route)
    }
}