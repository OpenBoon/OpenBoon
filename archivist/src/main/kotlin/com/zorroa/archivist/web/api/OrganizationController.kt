package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.service.OrganizationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class OrganizationController @Autowired constructor(
        val organizationService: OrganizationService
){

    private val v1ApiRoot = "/api/v1/organizations"

    @PostMapping(value = [v1ApiRoot])
    fun create(@RequestBody builder: OrganizationSpec) : Organization {
        return organizationService.create(builder)
    }

    @GetMapping(value = ["/api/v1/organizations/{id}"])
    operator fun get(@PathVariable id: String): Organization {
        return organizationService.get(UUID.fromString(id))
    }
}