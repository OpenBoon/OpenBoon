package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PermissionSpec
import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionServiceTests : AbstractTest() {

    @Test
    fun testCreate() {
        val perm = permissionService.createPermission(
                PermissionSpec().apply{description="foo"; name="shoe"; type="test"})
        assertEquals(perm.fullName, "test::shoe")
        assertEquals(perm.name, "shoe")
        assertEquals(perm.type, "test")
    }

    @Test
    fun testGet() {
        val perm1 = permissionService.createPermission(
                PermissionSpec().apply{description="foo"; name="shoe"; type="test"})
        val perm2 = permissionService.getPermission(perm1.id)
        assertEquals(perm1, perm2);
    }

    @Test
    fun testGetAll() {
        val perms = permissionService.getPermissions()
        assertTrue(perms.isNotEmpty())
    }

    @Test
    fun testGetAllNames() {
        val names = permissionService.getPermissionNames()
        assertTrue(names.isNotEmpty())
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testDelete() {
        val perm1 = permissionService.createPermission(
                PermissionSpec().apply{description="foo"; name="shoe"; type="test"})
        assertTrue(permissionService.deletePermission(perm1))
        assertFalse(permissionService.deletePermission(perm1))
        permissionService.getPermission(perm1.id)
    }
}
