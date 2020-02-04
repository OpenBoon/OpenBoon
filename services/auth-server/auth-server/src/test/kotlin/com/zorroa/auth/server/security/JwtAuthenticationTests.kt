package com.zorroa.auth.server.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.auth.server.AbstractTest
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.apikey.SigningKey
import com.zorroa.zmlp.util.Json
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class JwtAuthenticationTests : AbstractTest() {

    @Autowired
    lateinit var jwtAuthenticationFilter: JWTAuthorizationFilter

    @Test
    fun testValidateToken() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val apiKey = apiKeyService.create(spec)
        val token = apiKey.getValidationKey().getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals(auth.user.id, apiKey.id)
    }

    @Test
    fun testValidateInceptionToken() {
        val apiKey = Json.Mapper.readValue<SigningKey>(File("src/test/resources/key.json"))
        val token = apiKey.getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals("admin-key", auth.user.name)
        assertEquals(UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"),
            auth.user.projectId)
    }

    @Test
    fun testValidateInceptionTokenWithProjectHeader() {
        val apiKey = Json.Mapper.readValue<SigningKey>(File("src/test/resources/key.json"))
        val token = apiKey.getJwtToken()
        val pid = UUID.randomUUID()

        val auth = jwtAuthenticationFilter.validateToken(token, pid)
        assertEquals(auth.user.projectId, pid)
        assertEquals("admin-key", auth.user.name)
    }
}
