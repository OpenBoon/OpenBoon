package com.zorroa.archivist.web.api;

/**
 * Created by chambers on 10/28/15.
 */

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.archivist.service.AssetService;
import com.zorroa.archivist.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
@RestController
public class PermissionController {

    @Autowired
    UserService userService;

    @Autowired
    AssetService assetService;

    /**
     * Get a particular permission record.
     */
    @RequestMapping(value="/api/v1/permissions/{id}", method = RequestMethod.GET)
    public Permission get(@PathVariable String id) {
        return userService.getPermission(id);
    }

    /**
     * Return all available permissions.
     */
    @RequestMapping(value="/api/v1/permissions", method = RequestMethod.GET)
    public List<Permission> getAll() {
        return userService.getPermissions();
    }

    /**
     * Return all available permissions.
     */
    @RequestMapping(value="/api/v1/permissions/_names", method = RequestMethod.GET)
    public List<String> getAllNames() {
        return userService.getPermissionNames();
    }

    /**
     * Create a new permission.
     */
    @RequestMapping(value="/api/v1/permissions", method = RequestMethod.POST)
    public Permission create(@RequestBody PermissionSpec builder) {
        return userService.createPermission(builder);
    }

    /**
     * Delete a permission.
     */
    @RequestMapping(value="/api/v1/permissions/{id}", method = RequestMethod.DELETE)
    public Object delete(@PathVariable String id) {
        Permission p = userService.getPermission(id);
        return HttpUtils.status("permissions", id, "delete", userService.deletePermission(p));
    }
}
