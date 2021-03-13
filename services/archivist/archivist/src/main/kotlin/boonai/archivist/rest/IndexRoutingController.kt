package boonai.archivist.rest

import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.IndexRouteSimpleSpec
import boonai.archivist.domain.IndexRouteSpec
import boonai.archivist.domain.IndexTask
import boonai.archivist.domain.IndexToIndexMigrationSpec
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.IndexRoutingService
import boonai.archivist.service.IndexTaskService
import boonai.archivist.util.HttpUtils
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
import springfox.documentation.annotations.ApiIgnore
import java.util.UUID

@PreAuthorize("hasAuthority('SystemManage')")
@RestController
@ApiIgnore
class IndexRoutingController @Autowired constructor(
    val indexRoutingService: IndexRoutingService,
    val indexTaskService: IndexTaskService
) {

    @PostMapping(value = ["/api/v1/index-routes"])
    fun create(@RequestBody spec: IndexRouteSpec): IndexRoute {
        return indexRoutingService.createIndexRoute(spec)
    }

    @PostMapping(value = ["/api/v2/index-routes"])
    fun createV2(@RequestBody spec: IndexRouteSimpleSpec): IndexRoute {
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

    @GetMapping(value = ["/api/v1/index-routes/{id}/_attrs"])
    fun getState(@PathVariable id: UUID): Map<String, Any> {
        val route = indexRoutingService.getIndexRoute(id)
        return indexRoutingService.getEsIndexState(route)
    }

    @PutMapping(value = ["/api/v1/index-routes/{id}/_close"])
    fun closeIndex(@PathVariable id: UUID): Any {
        val route = indexRoutingService.getIndexRoute(id)
        return HttpUtils.status("index-route", "close", indexRoutingService.closeIndex(route))
    }

    @DeleteMapping(value = ["/api/v1/index-routes/{id}"])
    fun deleteIndex(@PathVariable id: UUID): Any {
        val route = indexRoutingService.getIndexRoute(id)
        return HttpUtils.status("index-route", "delete", indexRoutingService.deleteIndex(route))
    }

    @PutMapping(value = ["/api/v1/index-routes/{id}/_open"])
    fun openIndex(@PathVariable id: UUID): Any {
        val route = indexRoutingService.getIndexRoute(id)
        return HttpUtils.status("index-route", "close", indexRoutingService.openIndex(route))
    }

    @PostMapping(value = ["/api/v1/index-routes/_migrate"])
    fun migrate(@RequestBody spec: IndexToIndexMigrationSpec): IndexTask {
        return indexTaskService.createIndexMigrationTask(spec)
    }
}
