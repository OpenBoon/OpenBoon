package com.zorroa.archivist.security

import com.zorroa.security.Groups
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
class UtilsTests {

    @Test
    @WithMockUser(authorities = [Groups.SUPERADMIN])
    fun superAdminHasAnyPermission() {
        assertTrue(hasPermission(Groups.SUPERADMIN))
        assertTrue(hasPermission("zorroa::foo"))
    }

    @Test
    @WithMockUser(authorities = [Groups.ADMIN])
    fun adminDoesNotHaveSuperadminPermission() {
        assertFalse(hasPermission(Groups.SUPERADMIN))
    }

    @Test
    @WithMockUser(authorities = [Groups.ADMIN])
    fun adminHasAnyPermissionExceptSuperadmin() {
        assertTrue(hasPermission(Groups.SUPERADMIN, Groups.LIBRARIAN))
        assertTrue(hasPermission("zorroa::foo"))
    }

    @Test
    @WithMockUser(authorities = [Groups.READ, Groups.EVERYONE])
    fun userHasPermission() {
        assertTrue(hasPermission(Groups.READ))
        assertTrue(hasPermission(Groups.EVERYONE))
    }

    @Test
    @WithMockUser(authorities = [Groups.READ, Groups.EVERYONE])
    fun userDoesNotHaveAnyPermission() {
        assertFalse(hasPermission("zorroa::foo"))
    }

    @Test
    @WithMockUser(authorities = [Groups.ADMIN])
    fun adminHasPermissionsEmptyIsTrue() {
        assertTrue(hasPermission())
    }

    @Test
    @WithMockUser(authorities = [Groups.EVERYONE])
    fun userHasPermissionsEmptyIsFalse() {
        assertFalse(hasPermission())
    }

    @Test
    fun unauthenticatedUser() {
        assertFalse(hasPermission())
        assertFalse(hasPermission(Groups.SUPERADMIN))
        assertFalse(hasPermission(Groups.EVERYONE))
    }
}
