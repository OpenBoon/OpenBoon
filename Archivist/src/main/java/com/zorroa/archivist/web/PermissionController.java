package com.zorroa.archivist.web;

/**
 * Created by chambers on 10/28/15.
 */

import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.PermissionBuilder;
import com.zorroa.archivist.sdk.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PermissionController {

    @Autowired
    UserService userService;

    /**
     * Get a paritcular permission record.
     */
    @RequestMapping(value="/api/v1/permissions/{id}", method= RequestMethod.GET)
    public Permission get(@PathVariable int id) {
        return userService.getPermission(id);
    }

    /**
     * Return all available permissions.
     */
    @RequestMapping(value="/api/v1/permissions", method= RequestMethod.GET)
    public List<Permission> getAll() {
        return userService.getPermissions();
    }

    /**
     * Create a new permission.
     */
    @RequestMapping(value="/api/v1/permissions", method= RequestMethod.POST)
    public Permission create(@RequestBody PermissionBuilder builder) {
        return userService.createPermission(builder);
    }
}
