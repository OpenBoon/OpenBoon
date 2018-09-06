package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.sdk.security.AuthSource
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRegistryServiceTests : AbstractTest() {

    @Test
    fun testGetUser() {
        val authed = AuthSource("IRM", "saml", "saml", groups=listOf("marketing", "sales"))
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        val user2 = userRegistryService.getUser("billybob@bob.com")
        assertEquals(user1.username, user2.username)
        assertEquals(user1.id, user2.id)
        assertEquals(user1.authorities.size, user2.authorities.size)
    }

    @Test
    fun testRegisterUserWithGroups() {
        val authed = AuthSource("IRM", "saml", "saml", groups=listOf("marketing", "sales"))
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
            it.authority == "saml::marketing"
        }))
        assertTrue(user.authorities.stream().anyMatch({
            it.authority == "saml::sales"
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
