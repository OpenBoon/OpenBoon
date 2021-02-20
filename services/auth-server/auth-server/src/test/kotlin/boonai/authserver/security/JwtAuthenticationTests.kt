package boonai.authserver.security

import com.fasterxml.jackson.module.kotlin.readValue
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import boonai.authserver.AbstractTest
import boonai.authserver.domain.ApiKeySpec
import boonai.common.apikey.AuthServerClient
import boonai.common.apikey.Permission
import boonai.common.apikey.SigningKey
import boonai.common.util.Json
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.util.UUID
import javax.servlet.http.HttpServletRequest
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
        val token = apiKey.getValidationKey().getJwtToken()

        val auth = jwtAuthenticationFilter.validateToken(token)
        assertEquals(auth.user.id, apiKey.id)
    }

    @Test
    fun testValidateTokenWithhAttrs() {
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
        assertEquals(
            UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"),
            auth.user.projectId
        )
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

    @Test
    fun testParamProjectIdParamOverride() {
        val req: HttpServletRequest = Mockito.mock(HttpServletRequest::class.java)
        val pid = UUID.randomUUID()
        whenever(req.getParameter(eq(AuthServerClient.PROJECT_ID_PARAM))).thenReturn(pid.toString())
        assertEquals(pid, jwtAuthenticationFilter.getProjectIdOverride(req))
    }

    @Test
    fun testParamProjectIdHeaderOverride() {
        val req: HttpServletRequest = Mockito.mock(HttpServletRequest::class.java)
        val pid = UUID.randomUUID()
        whenever(req.getHeader(eq(AuthServerClient.PROJECT_ID_HEADER))).thenReturn(pid.toString())
        assertEquals(pid, jwtAuthenticationFilter.getProjectIdOverride(req))
    }
}
