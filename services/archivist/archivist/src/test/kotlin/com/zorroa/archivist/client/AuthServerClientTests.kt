package com.zorroa.archivist.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.common.util.Json
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
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import java.net.URI
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestPropertySource(locations = ["classpath:test.properties", "classpath:jwt.properties"])
class AuthServerClientTests : AbstractTest() {

    lateinit var mockServer: MockRestServiceServer

    lateinit var authServerClient: AuthServerClient

    @Before
    fun init() {
        authServerClient = AuthServerClient("http://localhost:9090/auth/v1/auth-token")
        mockServer = MockRestServiceServer.createServer(authServerClient.rest)
    }

    @Test
    fun testAuthToken() {
        val token =
            "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0SWQiOiJDQzdDMkU2Ri0xRTM2LTQ3MzEtOTE1NC05NTk4RTIyNDA4QjciLCJrZXlJZCI6IjExRUFBRDE2LUQzNTUtNDAwNi05MEU0LTZCRDEwMEMzQ0Q4MSJ9.gH7dTALpd9eTr2TnfPlwu6RqAN-EFPycvHGIFZ-3J1q5GhLCwie0zDSjX4-yTZPYqheDYEsZaE5bYyT6GhfXZg"
        val payload = """
        {
            "projectId": "CC7C2E6F-1E36-4731-9154-9598E22408B7",
            "keyId": "11EAAD16-D355-4006-90E4-6BD100C3CD81"
        }
        """.trimIndent()

        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(URI("http://localhost:8181/auth/v1/auth-token"))
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer $token"))
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
            )

        val apiKey = authServerClient.authenticate(token)
        assertEquals("CC7C2E6F-1E36-4731-9154-9598E22408B7", apiKey.projectId.toString())
        assertEquals("11EAAD16-D355-4006-90E4-6BD100C3CD81", apiKey.keyId.toString())
    }
}
