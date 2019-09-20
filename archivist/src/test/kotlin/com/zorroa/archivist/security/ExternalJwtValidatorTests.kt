package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Access
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import java.net.URI
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestPropertySource(locations = ["classpath:test.properties", "classpath:jwt.properties"])
class ExternalJwtValidatorTests : AbstractTest() {

    lateinit var mockServer: MockRestServiceServer

    lateinit var validator: HttpExternalJwtValidator

    @Autowired
    lateinit var masterJwtValidator: MasterJwtValidator

    @Autowired
    lateinit var accessResolver: AccessResolver

    @Before
    fun init() {
        validator = HttpExternalJwtValidator(
            URI.create("http://localhost:8181/validate"),
            URI.create("http://localhost:8181/refresh"),
            userRegistryService
        )
        mockServer = MockRestServiceServer.createServer(validator.rest)
    }

    @Test
    fun testExternalValidatorConfigured() {
        assertNotNull(masterJwtValidator.externalJwtValidator)
    }

    @Test
    fun testEmbeddedQueryStringFilter() {
        authenticate("user")
        val user = getUser()
        userService.addPermissions(user, listOf(permissionService.getPermission(Groups.READ)))

        val id = user.id.toString()
        val token = JWT.create()
            .withClaim("userId", id)
            .withClaim("organizationId", user.organizationId.toString())
            .withClaim("queryStringFilter", "foo.bar:123")
            .sign(Algorithm.HMAC256(userService.getHmacKey(user)))

        val body = """{
                "userId": "$id",
                "organizationId": "${user.organizationId}",
                "queryStringFilter": "foo.bar:123"
            }
        """.trimIndent()

        mockServer.expect(
            ExpectedCount.once(),
            requestTo(URI("http://localhost:8181/validate"))
        )
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer $token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
            )

        val result = validator.validate(token)
        validator.provisionUser(result)

        // Take our validated claims and wrap them in a JwtAuthenticationToken
        val vjwt = JwtAuthenticationToken(validator.validate(token))

        // Call authenticate to run the JwtAuthenticationProvider clsass
        val auth = authenticationManager.authenticate(vjwt)
        println(Json.prettyString(auth))

        // Replaces the globally logged in user with our authenticated user.
        SecurityContextHolder.getContext().authentication = auth

        // Get a permission filter.
        val filter = accessResolver.getAssetPermissionsFilter(Access.Read).toString()
        assertTrue(filter.contains("query_string"))
    }

    @Test
    fun testValidateAndProvision() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1NjQ3NjE0NzcsImV4cCI6MTU5NjI5NzQ3NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSJ9.6PaIyxGoG9HRqAdTpxq7sf4yJgnNKSLN-HPRCQYU3hU"
        val payload = """
            {
                "userId": "2B2ED9E0-294B-4847-A019-A4E8F46ADEE3",
                "username": "dilbert",
                "organizationId": "00000000-9998-8888-7777-666666666666",
                "permissions": "zorroa::librarian"
            }
        """.trimIndent()

        mockServer.expect(
            ExpectedCount.once(),
            requestTo(URI("http://localhost:8181/validate"))
        )
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer $token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
            )

        val result = validator.validate(token)
        val user = validator.provisionUser(result)

        assertEquals("dilbert", user?.username)
        assertEquals(UUID.fromString("00000000-9998-8888-7777-666666666666"), user?.organizationId)
        assertEquals(UUID.fromString("2B2ED9E0-294B-4847-A019-A4E8F46ADEE3"), user?.id)
        assertTrue(userRegistryService.exists("dilbert", "Jwt"))
        assertTrue(user?.authorities?.map { it.authority }!!.contains("zorroa::librarian"))

        // provision 1 more time to ensure the reprovisioning works.
        validator.provisionUser(result)
    }

    @Test
    fun testTokenRefresh() {
        val token = "abc123"
        val rspToken = "zaq321"

        mockServer.expect(
            ExpectedCount.once(),
            requestTo(URI("http://localhost:8181/refresh"))
        )
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer $token"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(rspToken)
            )

        val result = validator.refresh(token)
        assertEquals(result, rspToken)
    }
}