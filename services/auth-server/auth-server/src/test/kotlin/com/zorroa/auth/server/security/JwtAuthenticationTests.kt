package com.zorroa.auth.server.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.auth.server.AbstractTest
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.zmlp.apikey.ApiKey
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.util.Json
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
            setOf(Permission.AssetsRead)
        )
        val apiKey = apiKeyService.create(spec)
        val token = apiKey.getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals(auth.user.id, apiKey.id)
    }

    @Test
    fun testValidateInceptionToken() {
        val apiKey = Json.Mapper.readValue<ApiKey>(File("src/test/resources/key.json"))
        val token = apiKey.getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals(auth.user.id, apiKey.id)
    }

    @Test
    fun testValidateInceptionTokenWithProjectHeader() {
        val apiKey = Json.Mapper.readValue<ApiKey>(File("src/test/resources/key.json"))
        val token = apiKey.getJwtToken()
        val pid = UUID.randomUUID()

        val auth = jwtAuthenticationFilter.validateToken(token, pid)
        assertEquals(auth.user.id, apiKey.id)
        assertEquals(auth.user.projectId, pid)
    }
}