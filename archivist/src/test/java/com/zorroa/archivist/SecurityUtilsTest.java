package com.zorroa.archivist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Access;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.sdk.security.Groups;
import com.zorroa.archivist.security.UtilsKt;
import com.zorroa.archivist.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecurityUtilsTest extends AbstractTest {
    @Autowired
    UserService userService;

    @Test
    public void testHasPermissionWithAcl() {
        // Get the perms we have
        authenticate("user");
        List<Integer> perms = ImmutableList.copyOf(UtilsKt.getPermissionIds());

        // Create an ACL and add a Read permission
        Acl acl = new Acl();
        acl.addEntry(perms.get(0), Access.Read);

        // Test the has Permission function.
        assertTrue(UtilsKt.hasPermission(acl, Access.Read));
        assertFalse(UtilsKt.hasPermission(acl, Access.Write));
    }

    @Test
    public void testHasPermissionIsAdministrator() {
        Permission p = permissionService.getPermission(Groups.ADMIN);
        userService.setPermissions(UtilsKt.getUser(), Lists.newArrayList(p));

        // Reauthenticate the user, this sets up the admin's normal
        // permissions.
        authenticate("admin");

        // Create an ACL and add a Read permission
        List<Integer> perms = ImmutableList.copyOf(UtilsKt.getPermissionIds());
        Acl acl = new Acl();
        acl.addEntry(perms.get(0), Access.Read);

        // Test the hasPermission function.
        assertTrue(UtilsKt.hasPermission(acl, Access.Read));
        assertTrue(UtilsKt.hasPermission(acl, Access.Write));
    }
}
