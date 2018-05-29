package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserProfileUpdate
import com.zorroa.archivist.domain.UserSpec
import com.zorroa.archivist.sdk.security.Groups
import com.zorroa.sdk.client.exception.DuplicateEntityException
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserServiceTests : AbstractTest() {

    internal lateinit var testUser: User

    @Before
    fun init() {
        val builder = UserSpec("billybob",
                "123password!",
                "testing@zorroa.com",
                firstName="BillyBob",
                lastName = "Rodriquez")
        testUser = userService.create(builder)
    }

    @Test
    fun createUser() {
        val builder = UserSpec("test",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName = "Baggins")

        builder.permissionIds =
                listOf(permissionService.getPermission(Groups.MANAGER).id)

        val user = userService.create(builder)

        /*
         * Re-authenticate this test as the user we just made.  Otherwise we can't
         * access the user folder.
         */
        authenticate(user.username)

        /*
         * Get the user's personal folder.
         */
        val folder = folderService.get("/Users/test")
        assertEquals(folder!!.name, user.username)

        /*
         * Get the permissions for the user.  This should contain the user's
         * personal permission and the user group
         */
        val perms = userService.getPermissions(user)
        assertEquals(3, perms.size.toLong())
        assertTrue(userService.hasPermission(user, permissionService.getPermission(Groups.MANAGER)))
        assertTrue(userService.hasPermission(user, permissionService.getPermission("user::test")))
        assertFalse(userService.hasPermission(user, permissionService.getPermission(Groups.DEV)))
    }

    @Test(expected = DuplicateEntityException::class)
    fun createDuplicateUser() {
        val builder = UserSpec("test",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName = "Baggins")
        userService.create(builder)
        userService.create(builder)
    }

    @Test
    fun testSetEnabled() {
        val builder = UserSpec("test",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName="Baggins")
        val user = userService.create(builder)
        assertTrue(userService.setEnabled(user, false))
    }

    @Test
    fun setPermissions() {
        val builder = UserSpec("bilbob",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName="Baggins")
        builder.permissionIds = listOf(permissionService.getPermission(Groups.MANAGER).id)

        val user = userService.create(builder)
        val dev = permissionService.getPermission(Groups.DEV)
        assertTrue(userService.hasPermission(user, permissionService.getPermission(Groups.MANAGER)))
        assertFalse(userService.hasPermission(user, dev))

        userService.setPermissions(user, Lists.newArrayList(dev))
        assertFalse(userService.hasPermission(user, permissionService.getPermission(Groups.MANAGER)))
        assertTrue(userService.hasPermission(user, dev))
    }

    @Test(expected = IllegalArgumentException::class)
    fun updatePasswordFailureTooShort() {
        try {
            userService.resetPassword(testUser, "nogood")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("or more characters"))
            throw e
        }

    }

    @Test(expected = IllegalArgumentException::class)
    fun updatePasswordFailureNoCaps() {
        try {
            userService.resetPassword(testUser, "nogood")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("upper case"))
            throw e
        }

    }

    @Test(expected = IllegalArgumentException::class)
    fun updatePasswordFailureNoNumbers() {
        try {
            userService.resetPassword(testUser, "nogood")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("least 1 number."))
            throw e
        }

    }

    @Test
    fun updatePassword() {
        userService.resetPassword(testUser, "DogCatched1")
        userService.checkPassword(testUser.username, "DogCatched1")
    }

    @Test
    fun updateUser() {
        val builder = UserSpec("test",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName = "Baggins")
        val user = userService.create(builder)

        val update = UserProfileUpdate()
        update.username = "bilbo"
        update.firstName = "foo"
        update.lastName = "bar"
        update.email = "test@test.com"

        assertTrue(userService.update(user, update))
        val updated = userService.get(user.id)
        assertEquals(update.username, updated.username)
        assertEquals(update.email, updated.email)
        assertEquals(update.firstName, updated.firstName)
        assertEquals(update.lastName, updated.lastName)
    }

    @Test
    fun updateUsername() {
        val builder = UserSpec("bilbob",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName = "Baggins")
        val user = userService.create(builder)

        val update = UserProfileUpdate("gandalf",
                "gandalf@zorroa.com",
                "Bilbo","Baggins")

        assertTrue(folderService.exists("/Users/bilbob"))
        assertTrue(permissionService.permissionExists("user::bilbob"))
        assertTrue(userService.update(user, update))
        assertFalse(folderService.exists("/Users/bilbob"))
        assertFalse(permissionService.permissionExists("user::bilbob"))
        assertTrue(folderService.exists("/Users/gandalf"))
        assertTrue(permissionService.permissionExists("user::gandalf"))
    }

    @Test
    fun updateUsernameWithBlankUsername() {

        val update = UserProfileUpdate("",
                "gandalf@zorroa.com",
                "Bilbo","Baggins")

        assertTrue(userService.update(testUser, update))
        assertEquals(testUser.username, userService.get(testUser.id).username)
        assertTrue(folderService.exists("/Users/billybob"))
        assertTrue(permissionService.permissionExists("user::billybob"))
    }

    @Test
    fun testImmutablePermissions() {
        val builder = UserSpec("test",
                "123password",
                "test@test.com",
                firstName="Bilbo",
                lastName = "Baggins")
        builder.permissionIds = listOf(permissionService.getPermission(Groups.ADMIN).id)
        val sup = permissionService.getPermission(Groups.ADMIN)

        val admin = userService.get("admin")
        userService.setPermissions(admin, Lists.newArrayList(sup))

        for (p in userService.getPermissions(admin)) {
            if (p.name == "admin" && p.type == "user") {
                // We found the user permission, all good
                return
            }
        }
        throw RuntimeException("The admin user was missing the user::admin permission")
    }

}
