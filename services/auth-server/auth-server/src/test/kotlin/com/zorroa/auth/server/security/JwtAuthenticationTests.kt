package com.zorroa.auth.server.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.auth.client.ApiKey
import com.zorroa.auth.client.Json
import com.zorroa.auth.client.Permission
import com.zorroa.auth.server.AbstractTest
import com.zorroa.auth.server.domain.ApiKeySpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.util.UUID
import kotlin.test.assertEquals

class JwtAuthenticationTests : AbstractTest() {

    @Autowired
    lateinit var jwtAuthenticationFilter: JWTAuthorizationFilter

    @Test
    fun testValidateToken() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead),
            UUID.randomUUID()
        )
        val apiKey = apiKeyService.create(spec)
        val token = apiKey.getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals(auth.user.id, apiKey.id)
    }

    @Test
    fun testValidateInceptionToken() {
        val apiKey = Json.mapper.readValue<ApiKey>(File("src/test/resources/key.json"))
        val token = apiKey.getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals(auth.user.id, apiKey.id)
    }
}