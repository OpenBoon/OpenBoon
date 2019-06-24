package com.zorroa.archivist.rest

/**
 * Created by chambers on 10/28/15.
 */

import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.PermissionFilter
import com.zorroa.archivist.domain.PermissionSpec
import com.zorroa.archivist.service.IndexService
import com.zorroa.archivist.service.PermissionService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Timed
@Api(tags = ["Permission"], description = "Operations for interacting with Permissions.")
class PermissionController @Autowired constructor(
    val permissionService: PermissionService,
    val indexService: IndexService
) {

    @ApiOperation("Gets all Permissions.")
    @GetMapping(value = ["/api/v1/permissions"])
    fun getAll(): List<Permission> = permissionService.getPermissions()

    @ApiOperation("Gets a list of all Permission names.")
    @GetMapping(value = ["/api/v1/permissions/_names"])
    fun getAllNames(): List<String> = permissionService.getPermissionNames()

    @ApiOperation("Get a Permission.")
    @RequestMapping(value = ["/api/v1/permissions/{id}"])
    fun get(@ApiParam("UUID of the Permission.") @PathVariable id: UUID): Permission {
        return permissionService.getPermission(id)
    }

    @ApiOperation("Searches for a single Permission.",
        notes = "Throws an error if more than 1 result is returned based on the given filter.")
    @RequestMapping(value = ["/api/v1/permissions/_findOne"], method = [RequestMethod.POST, RequestMethod.GET])
    fun find(@RequestBody(required = false) filter: PermissionFilter?): Permission {
        return permissionService.findPermission(filter ?: PermissionFilter())
    }

    @ApiOperation("Determine if a Permission exists.")
    @RequestMapping(value = ["/api/v1/permissions/_exists/{name}"])
    fun exists(@ApiParam("Name of the Permission.") @PathVariable name: String): Boolean? {
        return permissionService.permissionExists(name)
    }

    @ApiOperation("Create a Permission.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value = ["/api/v1/permissions"])
    fun create(@ApiParam("Permission to create.") @RequestBody builder: PermissionSpec): Permission {
        return permissionService.createPermission(builder)
    }

    @ApiOperation("Delete a Permission.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @DeleteMapping(value = ["/api/v1/permissions/{id}"])
    fun delete(@ApiParam("UUID of the Permission.") @PathVariable id: String): Any {
        val p = permissionService.getPermission(id)
        return HttpUtils.status("permissions", id, "delete", permissionService.deletePermission(p))
    }
}
