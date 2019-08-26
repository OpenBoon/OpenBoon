package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.sdk.security.AuthSource
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRegistryServiceTests : AbstractTest() {

    @Test
    fun testGetUser() {
        val attrs = mutableMapOf("company_id" to "123")
        val authed =
            AuthSource("IRM", "saml", "saml", groups = listOf("marketing", "sales"), attrs = attrs)
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        val user2 = userRegistryService.getUser("billybob@bob.com")
        assertEquals(user1.username, user2.username)
        assertEquals(user1.id, user2.id)
        assertEquals(user1.authorities.size, user2.authorities.size)
        assertEquals(user1.attrs, user2.attrs)
    }

    @Test
    fun testRegisterUserWithGroups() {
        val authed = AuthSource("IRM", "saml", "saml", groups = listOf("anim", "comp"))
        val user = userRegistryService.registerUser("billybob@bob.com", authed)

        assertEquals(user.username, "billybob@bob.com")

        // Test the core permissions exist.
        assertTrue(user.authorities.stream().anyMatch {
            it.authority == "user::billybob@bob.com"
        })
        assertTrue(user.authorities.stream().anyMatch {
            it.authority == "zorroa::everyone"
        })

        // Test we got the imported permissions
        assertTrue(user.authorities.stream().anyMatch {
            it.authority == "saml::anim"
        })
        assertTrue(user.authorities.stream().anyMatch {
            it.authority == "saml::comp"
        })
    }

    @Test
    fun testRegisterUserWithGroupsTwice() {
        val authed = AuthSource("IRM", "saml", "saml", groups = listOf("marketing", "sales"))
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        val user2 = userRegistryService.registerUser("billybob@bob.com", authed)
        assertEquals(user1.username, user2.username)
        assertEquals(user1.id, user2.id)
        assertEquals(user1.authorities.size, user2.authorities.size)
    }

    @Test
    fun testRegisterWithMappedGroups() {
        val authed =
            AuthSource("IRM", "saml", "irm", groups = listOf("marketing", "sales", "pigman", "boo"))
        val user1 = userRegistryService.registerUser("billybob@bob.com", authed)
        assertTrue(userService.hasPermission(user1, "zorroa", "librarian"))
        assertTrue(userService.hasPermission(user1, "zorroa", "administrator"))
        assertTrue(userService.hasPermission(user1, "irm", "pigman"))
        assertTrue(userService.hasPermission(user1, "zorroa", "everyone"))
    }

    @Test
    fun testRegisterUserWithNullGroups() {
        val authed = AuthSource("IRM", "saml", "saml")
        val user = userRegistryService.registerUser("billybob@bob.com", authed)

        assertEquals(user.username, "billybob@bob.com")

        // Test the core permissions exist.
        assertTrue(user.authorities.stream().anyMatch {
            it.authority == "user::billybob@bob.com"
        })
        assertTrue(user.authorities.stream().anyMatch {
            it.authority == "zorroa::everyone"
        })
    }

    @Test
    fun testGetEmail() {
        val registry = userRegistryService as UserRegistryServiceImpl

        var source = AuthSource("test", "saml", "saml")

        assertEquals("bob@zorroa.com", registry.getEmail("bob", source))
        assertEquals("jim@spock.com", registry.getEmail("jim@spock.com", source))

        // Username is email but the SAML 'mail' attribute overrides it.
        source = AuthSource("test", "saml", "saml", attrs = mapOf("mail" to "kirk@spock.com"))
        assertEquals("kirk@spock.com", registry.getEmail("bones@spock.com", source))
    }

    @Test
    fun testRegisterUserWithLanguage() {
        val username = "billybob@bob.com"
        val language = "jp"
        userRegistryService.registerUser(
            username,
            AuthSource("IRM", "saml", "saml", attrs = mapOf("user_locale" to language))
        )
        assertEquals(language, userService.get(username).language)
    }

    @Test
    fun testRegisterUserWithUserId() {
        val username = "billybob@bob.com"
        val userId = UUID.randomUUID()
        val user = userRegistryService.registerUser(
            username,
            AuthSource("IRM", "saml", "saml", userId = userId)
        )
        assertEquals(userId, user.id)
    }

    @Test
    fun testUpdateRegisteredUserLanguage() {
        val username = "billybob@bob.com"
        userRegistryService.registerUser(
            username,
            AuthSource("IRM", "saml", "saml", attrs = mapOf("user_locale" to "jp"))
        )
        assertEquals("jp", userService.get(username).language)

        userRegistryService.registerUser(
            username,
            AuthSource("IRM", "saml", "saml", attrs = mapOf("user_locale" to "es"))
        )
        assertEquals("es", userService.get(username).language)
    }

    @Test
    fun testUpdateRegisteredUserWithoutAttr() {
        val username = "billybob@bob.com"
        registerWithAuthAttrs(username, mapOf("user_locale" to "jp"))
        Assertions.assertThat(userService.get(username))
            .hasFieldOrPropertyWithValue("language", "jp")
            .hasFieldOrPropertyWithValue("firstName", "First")

        registerWithAuthAttrs(username, mapOf("first_name" to "foo"))
        Assertions.assertThat(userService.get(username))
            .hasFieldOrPropertyWithValue("language", "jp")
            .hasFieldOrPropertyWithValue("firstName", "foo")
    }

    @Test
    fun testUpdateRegisteredUserAuthAttrs() {
        val username = "billybob@bob.com"
        registerWithAuthAttrs(username, mapOf("foo" to "bar"))
        Assertions.assertThat(userService.get(username).attrs).isEqualTo(mapOf("foo" to "bar"))

        registerWithAuthAttrs(username, mapOf())
        Assertions.assertThat(userService.get(username).attrs).isEmpty()

        registerWithAuthAttrs(username, mapOf("foo" to "bar"))
        registerWithAuthAttrs(username, mapOf("baz" to "bing"))
        Assertions.assertThat(userService.get(username).attrs).isEqualTo(mapOf("baz" to "bing"))

        registerWithAuthAttrs(username, attrs = emptyMap())
        Assertions.assertThat(userService.get(username).attrs).isEmpty()
    }

    private fun registerWithAuthAttrs(username: String, attrs: Map<String, String>) {
        userRegistryService.registerUser(
            username,
            AuthSource("IRM", "saml", "saml", attrs = attrs)
        )
    }
}
