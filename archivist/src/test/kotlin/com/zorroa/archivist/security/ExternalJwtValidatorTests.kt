package com.zorroa.archivist.security

import com.zorroa.archivist.AbstractTest
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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

class ExternalJwtValidatorTests : AbstractTest() {

    lateinit var mockServer: MockRestServiceServer

    lateinit var validator: HttpExternalJwtValidator

    @Autowired
    lateinit var masterJwtValidator: MasterJwtValidator

    @Before
    fun init() {
        validator = HttpExternalJwtValidator(
            URI.create("http://localhost:8181/validate"), userRegistryService
        )
        mockServer = MockRestServiceServer.createServer(validator.rest)
    }

    @Test
    fun testExternalValidatorConfigured() {
        assertNotNull(masterJwtValidator.externalJwtValidator)
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
}