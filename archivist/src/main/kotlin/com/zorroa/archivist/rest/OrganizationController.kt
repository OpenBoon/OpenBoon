package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationFilter
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.OrganizationUpdateSpec
import com.zorroa.archivist.service.OrganizationService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SUPERADMIN)")
@Api(tags = ["Organization"], description = "Operations for interacting with Organizations.")
class OrganizationController @Autowired constructor(
    val organizationService: OrganizationService
) {

    @ApiOperation("Create an Organization.")
    @PostMapping(value = ["/api/v1/organizations"])
    fun create(@RequestBody spec: OrganizationSpec): Organization {
        return organizationService.create(spec)
    }

    @ApiOperation("Update an Organization.")
    @PutMapping(value = ["/api/v1/organizations/{id}"])
    fun update(
        @ApiParam("UUID of the Organization.") @PathVariable id: UUID,
        @ApiParam("Organization updates.") @RequestBody spec: OrganizationUpdateSpec
    ): Any {
        val org = organizationService.get(id)
        val result = organizationService.update(org, spec)
        return HttpUtils.updated("organization", org.id, result, organizationService.get(id))
    }

    @ApiOperation("Get an Organization.")
    @GetMapping(value = ["/api/v1/organizations/{id}"])
    operator fun get(@ApiParam("UUID of the Organization.") @PathVariable id: UUID): Organization {
        return organizationService.get(id)
    }

    @ApiOperation("Search for a single Organization.",
        notes = "Throws an error if more than 1 result is returned based on the given filter.")
    @RequestMapping(value = ["/api/v1/organizations/_findOne"], method = [RequestMethod.GET, RequestMethod.POST])
    fun find(@ApiParam("Search filter.") @RequestBody(required = false) filter: OrganizationFilter?): Organization {
        return organizationService.findOne(filter ?: OrganizationFilter())
    }

    @ApiOperation("Search for Organizations.")
    @RequestMapping(value = ["/api/v1/organizations/_search"], method = [RequestMethod.GET, RequestMethod.POST])
    fun search(
        @ApiParam("Search filter.") @RequestBody(required = false) req: OrganizationFilter?,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): KPagedList<Organization> {
        val filter = req ?: OrganizationFilter()
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return organizationService.getAll(filter)
    }
}
