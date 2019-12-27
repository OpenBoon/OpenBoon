package com.zorroa.archivist.client

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.clients.AuthServerClientImpl
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.security.Perm
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
import java.util.UUID
import kotlin.test.assertEquals

@TestPropertySource(locations = ["classpath:jwt.properties"])
class AuthServerClientTests : AbstractTest() {

    lateinit var mockServer: MockRestServiceServer

    @Before
    fun init() {
        authServerClient = AuthServerClientImpl("http://localhost:9090", null)
        mockServer = MockRestServiceServer.createServer(authServerClient.rest)
    }

    @Test
    fun testBase64Key() {
        val base64 = "ewogICJuYW1lIjogImFkbWluLWtleSIsCiAgInByb2plY3RJZCI6ICI1MDU1ME" +
            "FBQy02QzVBLTQxQ0QtQjc3OS0yODIxQkI1QjUzNUYiLAogICJrZXlJZCI6ICI3NjA5NDMxNy" +
            "1EOTY4LTQzQTgtQjlEQy1EMDY4MEE4OTlBRDciLAogICJzaGFyZWRLZXkiOiAicGNla2pEVl9" +
            "pcFNNWEFhQnFxdHE2Snd5NUZBTW5qZWhVUXJNRWhiRzhXMDFnaVZxVkxmRU45RmRNSXZ6dTByY" +
            "iIsCiAgInBlcm1pc3Npb25zIjogWyJTdXBlckFkbWluIl0KfQoK"
        val client = AuthServerClientImpl("http://localhost:9090", base64)
        val serviceKey = client.serviceKey
        assertEquals(UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"), serviceKey?.projectId)
        assertEquals(UUID.fromString("76094317-D968-43A8-B9DC-D0680A899AD7"), serviceKey?.id)
    }

    @Test
    fun testLoadKeyFile() {
        val client = AuthServerClientImpl("http://localhost:9090", "src/test/resources/inception-key.json")
        val serviceKey = client.serviceKey
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), serviceKey?.projectId)
        assertEquals(UUID.fromString("4338a83f-a920-40ab-a251-a123b17df1ba"), serviceKey?.id)
    }

    @Test
    fun testCreateApiKey() {
        val payload = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "id": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "sharedKey": "abc123"
        }
        """.trimIndent()

        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(URI("http://localhost:9090/auth/v1/apikey"))
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
            )

        val project = projectService.findOne(ProjectFilter())
        val apiKey = authServerClient.createApiKey(project, "test", listOf(Perm.ASSETS_WRITE))
        assertEquals("cc7c2e6f-1e36-4731-9154-9598e22408b7", apiKey.projectId.toString())
        assertEquals("11eaad16-d355-4006-90e4-6bd100c3cd81", apiKey.id.toString())
        assertEquals("abc123", apiKey.sharedKey)
    }

    @Test
    fun testGetApiKey() {
        val payload = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "id": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "sharedKey": "abc123"
        }
        """.trimIndent()

        mockServer.expect(
            ExpectedCount.once(),
            MockRestRequestMatchers.requestTo(URI("http://localhost:9090/auth/v1/apikey/_findOne"))
        )
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
            )

        val apiKey = authServerClient.getApiKey(UUID.randomUUID(), "test")
        assertEquals("cc7c2e6f-1e36-4731-9154-9598e22408b7", apiKey.projectId.toString())
        assertEquals("11eaad16-d355-4006-90e4-6bd100c3cd81", apiKey.id.toString())
        assertEquals("abc123", apiKey.sharedKey)
    }

    @Test
    fun testAuthToken() {
        val token =
            "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0SWQiOiJDQzdDMkU2Ri0xRTM2LTQ3MzEtOTE1NC05NTk4RTIyNDA4QjciLCJrZXlJZCI6IjExRUFBRDE2LUQzNTUtNDAwNi05MEU0LTZCRDEwMEMzQ0Q4MSJ9.gH7dTALpd9eTr2TnfPlwu6RqAN-EFPycvHGIFZ-3J1q5GhLCwie0zDSjX4-yTZPYqheDYEsZaE5bYyT6GhfXZg"
        val payload = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "id": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "name": "roflcopter",
            "permissions": ["SuperAdmin"]
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
    }
}
