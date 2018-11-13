package com.zorroa.archivist.web.api

/**
 * Created by chambers on 10/28/15.
 */

import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.PermissionSpec
import com.zorroa.archivist.service.IndexService
import com.zorroa.archivist.service.PermissionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
class PermissionController @Autowired constructor(
        val permissionService: PermissionService,
        val indexService: IndexService
){
    /**
     * Return all available permissions.
     */
    @GetMapping(value = ["/api/v1/permissions"])
    fun getAll() : List<Permission> = permissionService.getPermissions()

    /**
     * Return all available permissions.
     */
    @GetMapping(value = ["/api/v1/permissions/_names"])
    fun getAllNames() : List<String> = permissionService.getPermissionNames()

    /**
     * Get a particular permission record.
     */
    @RequestMapping(value = ["/api/v1/permissions/{id}"])
    fun get(@PathVariable id: String): Permission {
        return permissionService.getPermission(id)
    }

    /**
     * Return true if permission exits.
     */
    @RequestMapping(value = ["/api/v1/permissions/_exists/{name}"])
    fun exists(@PathVariable name: String): Boolean? {
        return permissionService.permissionExists(name)
    }

    /**
     * Create a new permission.
     */
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value = ["/api/v1/permissions"])
    fun create(@RequestBody builder: PermissionSpec): Permission {
        return permissionService.createPermission(builder)
    }

    /**
     * Delete a permission.
     */
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @DeleteMapping(value = ["/api/v1/permissions/{id}"])
    fun delete(@PathVariable id: String): Any {
        val p = permissionService.getPermission(id)
        return HttpUtils.status("permissions", id, "delete", permissionService.deletePermission(p))
    }
}
