package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.sdk.security.Groups
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PermissionDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var permissionDao: PermissionDao

    @Autowired
    internal lateinit var userDao: UserDao

    internal lateinit var perm: Permission

    internal lateinit var user: User

    @Before
    fun init() {
        val b = PermissionSpec("project", "avatar")
        b.description = "Access to the Avatar project"
        perm = permissionDao.create(b, false)

        val ub = testUserSpec()
        user = userService.create(ub)
    }

    @Test
    fun testCreate() {
        val b = PermissionSpec("group", "test")
        b.description = "test"

        val p = permissionDao.create(b, false)
        assertEquals(p.name, b.name)
        assertEquals(p.description, b.description)
    }

    @Test(expected = DuplicateKeyException::class)
    fun testCreateDuplicate() {
        val b = PermissionSpec("group", "test")
        b.description = "test"

        permissionDao.create(b, false)
        permissionDao.create(b, false)
    }

    @Test
    fun testCreateImmutable() {
        val b = PermissionSpec("foo", "bar")
        b.description = "foo bar"
        val p = permissionDao.create(b, true)
        assertTrue(p.isImmutable)
    }

    @Test
    fun testExists() {
        assertFalse(permissionDao.exists("foo::bar"))
        val b = PermissionSpec("foo", "bar")
        b.description = "foo bar"
        val p = permissionDao.create(b, true)
        assertTrue(permissionDao.exists("foo::bar"))
    }

    @Test
    fun testRenameUserPermission() {
        assertTrue(permissionDao.renameUserPermission(user, "rambo"))
        assertFalse(userDao.hasPermission(user, "user", "test"))
        assertTrue(userDao.hasPermission(user, "user", "rambo"))
    }

    @Test
    fun testGetByNameAndType() {
        val p = permissionDao.get("user", "test")
        assertTrue(p.isImmutable)
        assertEquals("user", p.type)
        assertEquals("test", p.name)
    }

    @Test
    fun testCount() {
        val count = permissionDao.count()
        val b = PermissionSpec("foo", "bar")
        permissionDao.create(b, true)
        assertEquals(count + 1, permissionDao.count())
    }

    @Test
    fun testCountWithFilter() {
        val count = permissionDao.count(PermissionFilter().setTypes(Sets.newHashSet("user")))
        assertTrue(count > 0)

        val b = PermissionSpec("foo", "bar")
        permissionDao.create(b, true)

        val newCount = permissionDao.count(PermissionFilter().setTypes(Sets.newHashSet("user")))
        assertEquals(count, newCount)
    }

    @Test
    fun testGet() {
        val p = permissionDao.get(perm.id)
        assertEquals(perm.name, p.name)
        assertEquals(perm.description, p.description)
    }

    @Test
    fun testGetAll() {
        val perms = permissionDao.getAll()
        assertTrue(perms.size > 0)
    }

    @Test
    fun testGetAllByNames() {
        val perms = permissionDao.getAll(ImmutableList.of("user::admin", Groups.EVERYONE))
        assertEquals(2, perms.size.toLong())
    }

    @Test
    fun testGetPagedEmptyFilter() {
        val perms = permissionDao.getPaged(Pager.first(), PermissionFilter())
        assertTrue(perms.size() > 0)
    }

    @Test
    fun testGetPagedFiltered() {
        val b = PermissionSpec("test1", "test2")
        b.description = "test"
        permissionDao.create(b, false)

        var perms = permissionDao.getPaged(Pager.first(),
                PermissionFilter().setTypes(Sets.newHashSet("test1")))
        assertEquals(1, perms.size().toLong())

        perms = permissionDao.getPaged(Pager.first(),
                PermissionFilter().setNames(Sets.newHashSet("test2")))
        assertEquals(1, perms.size().toLong())

        perms = permissionDao.getPaged(Pager.first(),
                PermissionFilter().setNames(Sets.newHashSet("test2"))
                        .setTypes(Sets.newHashSet("test1")))
        assertEquals(1, perms.size().toLong())
    }

    @Test
    fun testGetPagedSorted() {
        val b = PermissionSpec("test1", "test2")
        b.description = "test"
        permissionDao.create(b, false)

        assertTrue(permissionDao.getPaged(Pager.first(),
                PermissionFilter(ImmutableMap.of("id", "asc"))).size() > 0)

        assertTrue(permissionDao.getPaged(Pager.first(),
                PermissionFilter(ImmutableMap.of("name", "asc"))).size() > 0)

        assertTrue(permissionDao.getPaged(Pager.first(),
                PermissionFilter(ImmutableMap.of("type", "asc"))).size() > 0)

        assertTrue(permissionDao.getPaged(Pager.first(),
                PermissionFilter(ImmutableMap.of("description", "asc"))).size() > 0)
    }

    @Test
    fun testGetAllByType() {
        val perms = permissionDao.getAll("user")
        logger.info(Json.prettyString(perms))
        /*
         * There are 3 active users in this test: admin, user, and test.
         */
        assertEquals(4, perms.size.toLong())
    }

    @Test
    fun testGetAllByIds() {
        val perms1 = permissionDao.getAll()
        val perms2 = permissionDao.getAll(listOf(perms1[0].id, perms1[1].id))
        assertEquals(2, perms2.size.toLong())
        assertTrue(perms2.contains(perms1[0]))
        assertTrue(perms2.contains(perms1[1]))
    }

    @Test
    fun testDelete() {
        /*
         * Internally managed permissions cannot be deleted in this way.
         */
        assertFalse(permissionDao.delete(permissionDao.get(Groups.MANAGER)))
        assertTrue(permissionDao.delete(permissionDao.get("project::avatar")))
    }

    @Test
    fun testUpdate() {
        val b = PermissionSpec("group", "test")
        b.description = "foo"
        var p = permissionDao.create(b, false)
        assertEquals("group", p.type)
        assertEquals("test", p.name)
        assertEquals("foo", p.description)

        p.type = "foo"
        p.name = "bar"
        p.description = "bing"

        p = permissionDao.update(p)
        assertEquals("foo", p.type)
        assertEquals("bar", p.name)
        assertEquals("bing", p.description)
    }

    @Test
    fun testAttemptUpdateImmutable() {
        var p = permissionDao.get("user", "test")
        p.type = "foo"
        p.name = "bar"
        p.description = "bing"

        p = permissionDao.update(p)
        assertEquals("user", p.type)
        assertEquals("test", p.name)
    }

    @Test
    fun resolveAcl() {
        var acl = Acl().addEntry(Groups.EVERYONE, 1)
        acl = permissionDao.resolveAcl(acl, false)
        assertNotNull(acl[0].permissionId)
    }

    @Test
    fun resolveAclDuplicates() {
        var acl = Acl().addEntry(Groups.EVERYONE, 1)
        acl.addEntry(Groups.EVERYONE, 3)
        acl = permissionDao.resolveAcl(acl, false)
        assertEquals(1, acl.size.toLong())
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun resolveAclFailure() {
        var acl = Acl().addEntry("zorroa::shizzle", 1)
        acl = permissionDao.resolveAcl(acl, false)
    }

    @Test
    fun resolveAclAutoCreate() {
        var acl = Acl().addEntry("zorroa::shizzle", 1)
        acl = permissionDao.resolveAcl(acl, true)
        assertNotNull(acl[0].permissionId)
    }
}
