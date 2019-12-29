package com.zorroa.auth.client

import java.util.UUID
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthServerClientTests {

    lateinit var mockServer: MockWebServer

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
    }

    @Test
    fun testInitWithBase64SigningKey() {
        val base64 = "ewogICJuYW1lIjogImFkbWluLWtleSIsCiAgInByb2plY3RJZCI6ICI1MDU1ME" +
            "FBQy02QzVBLTQxQ0QtQjc3OS0yODIxQkI1QjUzNUYiLAogICJrZXlJZCI6ICI3NjA5NDMxNy" +
            "1EOTY4LTQzQTgtQjlEQy1EMDY4MEE4OTlBRDciLAogICJzaGFyZWRLZXkiOiAicGNla2pEVl9" +
            "pcFNNWEFhQnFxdHE2Snd5NUZBTW5qZWhVUXJNRWhiRzhXMDFnaVZxVkxmRU45RmRNSXZ6dTByY" +
            "iIsCiAgInBlcm1pc3Npb25zIjogWyJTdXBlckFkbWluIl0KfQoK"

        val client = AuthServerClient("http://localhost:9090", base64)
        val serviceKey = client.serviceKey
        assertEquals(UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"), serviceKey?.projectId)
        assertEquals(UUID.fromString("76094317-D968-43A8-B9DC-D0680A899AD7"), serviceKey?.keyId)
    }

    @Test
    fun testLoadKeyFile() {
        val client = AuthServerClient("http://localhost:9090", "src/test/resources/signing-key.json")
        val serviceKey = client.serviceKey
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), serviceKey?.projectId)
        assertEquals(UUID.fromString("4338a83f-a920-40ab-a251-a123b17df1ba"), serviceKey?.keyId)
    }

    @Test
    fun testCreateApiKey() {
        val responseBody = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "keyId": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "sharedKey": "abc123",
            "name": "foo",
            "permissions": ["AssetsImport"]
        }
        """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClient(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val apikey = client.createApiKey(
            UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"),
            "foo", listOf(Permission.AssetsImport)
        )

        assertEquals(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), apikey.projectId)
        assertEquals(UUID.fromString("11eaad16-d355-4006-90e4-6bd100c3cd81"), apikey.keyId)
        assertEquals("foo", apikey.name)
        assertEquals(setOf(Permission.AssetsImport), apikey.permissions)
    }

    @Test
    fun testGetApiKey() {
        val responseBody = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "keyId": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "sharedKey": "abc123",
            "name": "foo",
            "permissions": ["AssetsImport"]
        }
        """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClient(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val apikey = client.getApiKey(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), "foo")

        assertEquals(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), apikey.projectId)
        assertEquals(UUID.fromString("11eaad16-d355-4006-90e4-6bd100c3cd81"), apikey.keyId)
        assertEquals("foo", apikey.name)
        assertEquals(setOf(Permission.AssetsImport), apikey.permissions)
    }

    @Test
    fun testAuthToken() {
        val token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0SWQiOiJDQzdDMkU2Ri0xRTM2LT" +
            "Q3MzEtOTE1NC05NTk4RTIyNDA4QjciLCJrZXlJZCI6IjExRUFBRDE2LUQzNTUtNDAwNi05MEU0LTZCRDEwM" +
            "EMzQ0Q4MSJ9.gH7dTALpd9eTr2TnfPlwu6RqAN-EFPycvHGIFZ-3J1q5GhLCwie0zDSjX4-yTZPYqheDYEsZaE5bYyT6GhfXZg"

        val responseBody = """
        {
            "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
            "keyId": "11eaad16-d355-4006-90e4-6bd100c3cd81",
            "name": "foo",
            "permissions": ["AssetsImport"]
        }
        """.trimIndent()
        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClient(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val actor = client.authenticate(token)

        assertEquals(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), actor.projectId)
        assertEquals(UUID.fromString("11eaad16-d355-4006-90e4-6bd100c3cd81"), actor.keyId)
        assertEquals("foo", actor.name)
        assertEquals(setOf(Permission.AssetsImport), actor.permissions)
    }
}
