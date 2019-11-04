package com.zorroa.archivist.client

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.clients.AuthServerClient
import org.junit.Before
import org.junit.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import java.net.URI
import kotlin.test.assertEquals

@TestPropertySource(locations = ["classpath:test.properties", "classpath:jwt.properties"])
class AuthServerClientTests : AbstractTest() {

    lateinit var mockServer: MockRestServiceServer

    lateinit var authServerClient: AuthServerClient

    @Before
    fun init() {
        authServerClient = AuthServerClient("http://localhost:9090")
        mockServer = MockRestServiceServer.createServer(authServerClient.rest)
    }

    @Test
    fun testAuthToken() {
        val token =
            "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0SWQiOiJDQzdDMkU2Ri0xRTM2LTQ3MzEtOTE1NC05NTk4RTIyNDA4QjciLCJrZXlJZCI6IjExRUFBRDE2LUQzNTUtNDAwNi05MEU0LTZCRDEwMEMzQ0Q4MSJ9.gH7dTALpd9eTr2TnfPlwu6RqAN-EFPycvHGIFZ-3J1q5GhLCwie0zDSjX4-yTZPYqheDYEsZaE5bYyT6GhfXZg"
        val payload = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "keyId": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "permissions": ["ROLE_SUPERADMIN"]
        }
        """.trimIndent()

        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(URI("http://localhost:9090/auth/v1/auth-token"))
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer $token"))
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
            )

        val apiKey = authServerClient.authenticate(token)
        assertEquals("cc7c2e6f-1e36-4731-9154-9598e22408b7", apiKey.projectId.toString())
        assertEquals("11eaad16-d355-4006-90e4-6bd100c3cd81", apiKey.keyId.toString())
    }
}
