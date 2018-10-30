package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.sdk.security.AuthSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRegistryServiceTests : AbstractTest() {

    @Test
    fun testGetUser() {
        val attrs = mapOf("company_id" to "123")
        val authed = AuthSource("IRM", "saml", "saml", groups=listOf("marketing", "sales"),
                attrs=attrs)
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        val user2 = userRegistryService.getUser("billybob@bob.com")
        assertEquals(user1.username, user2.username)
        assertEquals(user1.id, user2.id)
        assertEquals(user1.authorities.size, user2.authorities.size)
        assertEquals(attrs, user2.attrs)
    }

    @Test
    fun testRegisterUserWithGroups() {
        val authed = AuthSource("IRM", "saml", "saml", groups=listOf("anim", "comp"))
        val user = userRegistryService.registerUser("billybob@bob.com", authed)

        assertEquals(user.username,"billybob@bob.com")

        // Test the core permissions exist.
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "user::billybob@bob.com"
        }))
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "zorroa::everyone"
        }))

        // Test we got the imported permissions
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "saml::anim"
        }))
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "saml::comp"
        }))
    }

    @Test
    fun testRegisterUserWithGroupsTwice() {
        val authed = AuthSource("IRM", "saml", "saml", groups=listOf("marketing", "sales"))
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        val user2 = userRegistryService.registerUser("billybob@bob.com", authed)
        assertEquals(user1.username, user2.username)
        assertEquals(user1.id, user2.id)
        assertEquals(user1.authorities.size, user2.authorities.size)
    }

    @Test
    fun testRegisterWithMappedGroups() {
        val authed = AuthSource("IRM", "saml", "irm",
                groups=listOf("marketing", "sales", "pigman", "boo"))
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        assertTrue(userService.hasPermission(user1, "zorroa", "librarian"))
        assertTrue(userService.hasPermission(user1, "zorroa", "admin"))
        assertTrue(userService.hasPermission(user1, "irm", "pigman"))
        assertTrue(userService.hasPermission(user1, "zorroa", "everyone"))
    }

    @Test
    fun testRegisterUserWithNullGroups() {
        val authed = AuthSource("IRM", "saml", "saml")
        val user = userRegistryService.registerUser("billybob@bob.com", authed)

        assertEquals(user.username,"billybob@bob.com")

        // Test the core permissions exist.
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "user::billybob@bob.com"
        }))
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "zorroa::everyone"
        }))
    }


}
