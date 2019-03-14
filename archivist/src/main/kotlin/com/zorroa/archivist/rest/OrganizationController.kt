package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.service.OrganizationService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SUPERADMIN)")
class OrganizationController @Autowired constructor(
        val organizationService: OrganizationService
){

    @PostMapping(value = [v1ApiRoot])
    fun create(@RequestBody spec: OrganizationSpec) : Organization {
        return organizationService.create(spec)
    }

    @PutMapping(value = ["$v1ApiRoot/{id}"])
    fun update(@PathVariable id: UUID, @RequestBody spec: OrganizationUpdateSpec) : Any {
        val org = organizationService.get(id)
        val result =  organizationService.update(org, spec)
        return HttpUtils.updated("organization", org.id, result, organizationService.get(id))
    }

    @GetMapping(value = ["$v1ApiRoot/{id}"])
    operator fun get(@PathVariable id: UUID): Organization {
        return organizationService.get(id)
    }

    /**
     * Find a single Organization record using a filter.
     */
    @RequestMapping(value = ["$v1ApiRoot/_findOne"], method = [RequestMethod.GET, RequestMethod.POST])
    fun find(@RequestBody(required = false) filter: OrganizationFilter?): Organization {
        return organizationService.findOne(filter ?: OrganizationFilter())
    }

    /**
     * Search Organizations using a filter.
     */
    @RequestMapping(value = ["$v1ApiRoot/_search"], method = [RequestMethod.GET, RequestMethod.POST])
    fun search(@RequestBody(required = false) req: OrganizationFilter?,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): KPagedList<Organization> {
        val filter = req ?: OrganizationFilter()
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return organizationService.getAll(filter)
    }

    companion object {
        const val v1ApiRoot = "/api/v1/organizations"
    }
}
