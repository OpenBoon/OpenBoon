package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.SuperAdminAuthentication
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.*

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
        assertTrue(p.immutable)
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
        assertTrue(p.immutable)
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
        val count = permissionDao.count(PermissionFilter(types=listOf("user")))
        assertTrue(count > 0)

        val b = PermissionSpec("foo", "bar")
        permissionDao.create(b, true)

        val newCount = permissionDao.count(PermissionFilter(types=listOf("user")))
        assertEquals(count, newCount)
    }

    @Test
    fun testGetId() {
        val p = permissionDao.getId(perm.authority)
        assertEquals(p, perm.id)
    }

    @Test
    fun testGetById() {
        val p = permissionDao.get(perm.id)
        assertEquals(perm.name, p.name)
        assertEquals(perm.description, p.description)
    }

    @Test
    fun testFindOne() {
        val p = permissionDao.findOne(PermissionFilter(authorities = listOf(perm.fullName)))
        assertEquals(perm.name, p.name)
        assertEquals(perm.description, p.description)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testFindOneFailure() {
        permissionDao.findOne(PermissionFilter(authorities = listOf("bob_dole")))
    }

    @Test
    fun testGetByAuthority() {
        val p = permissionDao.get(perm.authority)
        assertEquals(perm.name, p.name)
        assertEquals(perm.description, p.description)
    }

    @Test
    fun testGetAll() {
        val perms = permissionDao.getAll()

        val org = organizationService.create(OrganizationSpec("new-org"))
        SecurityContextHolder.getContext().authentication = SuperAdminAuthentication(org.id)
        val perms2 = permissionDao.getAll()

        assertHaveDifferentAdministratorPermission(perms, perms2)
    }

    private fun assertHaveDifferentAdministratorPermission(perms: List<Permission>, perms2: List<Permission>) {
        val administrator1 = perms.find { it.name == "administrator" }
        val administrator2 = perms2.find { it.name == "administrator" }

        assertNotNull(administrator2)
        assertNotNull(administrator1)
        assertNotEquals(administrator1.id, administrator2.id)
    }

    @Test
    fun testGetAllByNames() {
        val perms = permissionDao.getAll(ImmutableList.of("user::admin", Groups.EVERYONE))
        assertEquals(2, perms.size.toLong())
    }

    @Test
    fun testGetAllFiltered() {
        val perms = permissionDao.getAll(PermissionFilter())
        assertTrue(perms.size() > 0)
    }

    @Test
    fun testGetPagedFiltered() {
        val b = PermissionSpec("test1", "test2")
        b.description = "test"
        permissionDao.create(b, false)

        var perms = permissionDao.getAll(
                PermissionFilter(types=listOf("test1")))
        assertEquals(1, perms.size().toLong())

        perms = permissionDao.getAll(PermissionFilter(names=listOf("test2")))
        assertEquals(1, perms.size().toLong())

        perms = permissionDao.getAll(PermissionFilter(names=listOf("test2")))
        assertEquals(1, perms.size().toLong())
    }

    @Test
    fun testGetAllSorted() {
        // Just test the DB allows us to sort on each defined sortMap col
        for (field in PermissionFilter().sortMap.keys) {
            var filter = PermissionFilter().apply {
                sort = listOf("$field:a")
            }
            val page = permissionDao.getAll(filter)
            assertTrue(page.size() > 0)
        }
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

        val update = PermissionUpdateSpec(p.id, "foo", "bar", "bing")
        p = permissionDao.update(update)
        assertEquals("foo", p.type)
        assertEquals("bar", p.name)
        assertEquals("bing", p.description)
    }

    @Test
    fun testAttemptUpdateImmutable() {
        var p = permissionDao.get("user", "test")
        val update = PermissionUpdateSpec(p.id, "foo", "bar", "bing")

        p = permissionDao.update(update)
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

    @Test
    fun getAllPermissionSchema() {
        val schema = permissionDao.getDefaultPermissionSchema()
        assertTrue(schema.write.isNotEmpty())
        assertTrue(schema.read.isNotEmpty())
        assertTrue(schema.export.isNotEmpty())
    }
}
