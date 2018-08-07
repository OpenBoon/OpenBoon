package com.zorroa.archivist.repository

import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserProfileUpdate
import com.zorroa.common.domain.Pager
import com.zorroa.security.Groups
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.crypto.bcrypt.BCrypt
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var userDao: UserDao

    @Autowired
    internal lateinit var permissionDao: PermissionDao

    internal lateinit var user: User

    @Before
    fun init() {
        val builder = testUserSpec()
        builder.homeFolderId = UUID.randomUUID()
        builder.userPermissionId = permissionDao.get("zorroa", "manager").id
        user = userService.create(builder)
    }

    @Test
    fun testGet() {
        val (id) = userDao.get(user.id)
        assertEquals(user.id, id)
    }

    @Test
    fun testGetByUsername() {
        val user2 = userDao.get(user.username)
        val user3 = userDao.get(user.email)
        assertEquals(user2.id, user3.id)
        assertEquals(user2.toString(), user3.toString())
    }

    @Test
    fun testGetCount() {
        var count = userDao.getCount()
        assertEquals(count, userDao.getCount())
        val builder = testUserSpec("test2")
        builder.homeFolderId = UUID.randomUUID()
        builder.userPermissionId = permissionDao.get("zorroa", "manager").id
        user = userService.create(builder)
        assertEquals(++count, userDao.getCount())
    }

    @Test
    fun testAll() {
        assertEquals(4, userDao.getAll().size.toLong())

        val builder = testUserSpec("foo")
        builder.homeFolderId = UUID.randomUUID()
        builder.userPermissionId = permissionDao.get("zorroa", "manager").id
        userService.create(builder)

        assertEquals(5, userDao.getAll().size.toLong())
    }

    @Test
    fun testAllPageable() {
        assertEquals(4, userDao.getAll(Pager.first()).size().toLong())
        assertEquals(0, userDao.getAll(Pager(2, 4)).size().toLong())
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testGetFailed() {
        userDao.get("blah")
    }

    @Test
    fun testGetPassword() {
        // The crypted password
        val hashed = userDao.getPassword(user.username)
        val hashed2 = userDao.getPassword(user.email)
        assertEquals(hashed, hashed2)

        assertTrue(hashed.startsWith("$"))

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("test", hashed))
        assertFalse(BCrypt.checkpw("gtfo", hashed))
    }

    @Test
    fun testResetPassword() {
        assertTrue(userDao.setPassword(user, "fiddlesticks"))
        assertTrue(BCrypt.checkpw("fiddlesticks", userDao.getPassword(user.username)))
        assertFalse(BCrypt.checkpw("smeagol", userDao.getPassword(user.username)))
    }

    @Test
    fun testUpdate() {
        val builder = UserProfileUpdate()
        builder.firstName = "foo"
        builder.lastName = "bar"
        builder.email = "test@test.com"

        assertTrue(userDao.update(user, builder))
        val t = userDao.get(user.id)
        assertEquals(builder.email, t.email)
        assertEquals(builder.firstName, t.firstName)
        assertEquals(builder.lastName, t.lastName)
    }

    @Test
    fun testSetEnabled() {
        assertTrue(userDao.setEnabled(user, false))
        assertFalse(userDao.setEnabled(user, false))
    }

    @Test
    fun testExists() {
        assertTrue(userDao.exists(user.username, null))
        assertTrue(userDao.exists(user.username, "local"))
        assertFalse(userDao.exists(user.username, "ldap"))
        assertFalse(userDao.exists("sibawitzawis", null))
    }

    @Test
    fun testHasPermissionUningNames() {
        assertFalse(userDao.hasPermission(user, "zorroa", "manager"))
        userDao.addPermission(user, permissionDao.get(Groups.MANAGER), false)
        assertTrue(userDao.hasPermission(user, "zorroa", "manager"))
        assertFalse(userDao.hasPermission(user, "a", "b"))
    }

    @Test
    fun testHasPermission() {
        assertFalse(userDao.hasPermission(user, "zorroa", "manager"))
        userDao.addPermission(user, permissionDao.get(Groups.MANAGER), false)
        assertTrue(userDao.hasPermission(user, permissionDao.get("zorroa", "manager")))
        assertFalse(userDao.hasPermission(user, permissionDao.get("zorroa", "administrator")))
    }

    @Test
    fun testAddPermission() {
        userDao.addPermission(user, permissionDao.get(Groups.MANAGER), false)
        val perms = permissionDao.getAll(user)
        assertTrue(perms.contains(permissionDao.get(Groups.MANAGER)))
    }

    @Test
    fun testSetPermissions() {
        val p = permissionDao.get(Groups.MANAGER)
        assertEquals(1, userDao.setPermissions(user, Lists.newArrayList(p), "local").toLong())
        val perms = permissionDao.getAll(user)
        assertTrue(perms.contains(p))
    }

    @Test
    fun testGetUserByPasswordToken() {
        val token = userDao.setEnablePasswordRecovery(user)
        val (id) = userDao.getByToken(token)
        assertEquals(user.id, id)
    }

    @Test
    fun testSetForgotPassword() {
        val token = userDao.setEnablePasswordRecovery(user)
        assertEquals(64, token.length.toLong())
        assertEquals(token,
                jdbc.queryForObject("SELECT str_reset_pass_token FROM users WHERE pk_user=?",
                        String::class.java, user.id))
    }

    @Test
    fun testSetResetPassword() {
        assertFalse(userDao.resetPassword(user, "ABC123", "FOO"))
        val token = userDao.setEnablePasswordRecovery(user)

        assertFalse(userDao.resetPassword(user, "BAD_TOKEN", "FOO"))
        assertTrue(userDao.resetPassword(user, token, "FOO"))
    }

    @Test
    fun testIncrementLoginCount() {
        userDao.incrementLoginCounter(user)
        val t = userDao.get(user.id)
        assertTrue(t.timeLastLogin > 0)
        assertEquals(1, t.loginCount.toLong())

    }
}
