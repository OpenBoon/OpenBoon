package com.zorroa.archivist

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Permission
import com.zorroa.security.Groups
import com.zorroa.archivist.security.*
import com.zorroa.archivist.service.UserService

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class SecurityUtilsTest : AbstractTest() {

    @Test
    fun testHasPermissionWithAcl() {
        // Get the perms we have
        authenticate("user@zorroa.com")
        val perms = ImmutableList.copyOf(getPermissionIds())

        // Create an ACL and add a Read permission
        val acl = Acl()
        acl.addEntry(perms[0], Access.Read)

        // Test the has Permission function.
        assertTrue(hasPermission(acl, Access.Read))
        assertFalse(hasPermission(acl, Access.Write))
    }

    @Test
    fun testHasPermissionIsAdministrator() {
        val p = permissionService.getPermission(Groups.ADMIN)
        userService.setPermissions(getUser(), Lists.newArrayList(p))

        // Reauthenticate the user, this sets up the admin's normal
        // permissions.
        authenticate("admin")

        // Create an ACL and add a Read permission
        val perms = ImmutableList.copyOf(getPermissionIds())
        val acl = Acl()
        acl.addEntry(perms[0], Access.Read)

        // Test the hasPermission function.
        assertTrue(hasPermission(acl, Access.Read))
        assertTrue(hasPermission(acl, Access.Write))
    }
}
