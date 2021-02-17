package boonai.common.apikey

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
        val base64 = "ewogICAgImFjY2Vzc0tleSI6ICI0MzM4YTgzZmE5MjA0MGFiYTI1MWExMjNiMTdkZjFiYSIsCiAgICAic2VjcmV0" +
            "S2V5IjogInBjZWtqRFZfaXBTTVhBYUJxcXRxNkp3eTVGQU1uamVoVVFyTUVoYkc4VzAxZ2lWcVZMZkVOOUZkTUl2enUwcmIiCn0K"

        val client = AuthServerClientImpl("http://localhost:9090", base64)
        val serviceKey = client.serviceKey
        // assertEquals(UUID.fromString("50550AAC-6C5A-41CD-B779-2821BB5B535F"), serviceKey?.projectId)
        assertEquals("4338a83fa92040aba251a123b17df1ba", serviceKey?.accessKey)
        assertEquals("pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb", serviceKey?.secretKey)
    }

    @Test
    fun testLoadKeyFile() {
        val client = AuthServerClientImpl("http://localhost:9090", "src/test/resources/signing-key.json")
        val serviceKey = client.serviceKey
        // assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), serviceKey?.projectId)
        assertEquals("4338a83fa92040aba251a123b17df1ba", serviceKey?.accessKey)
    }

    @Test
    fun testCreateApiKey() {
        val responseBody =
            """
            {
                "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
                "id": "11eaad16-d355-4006-90e4-6bd100c3cd81",
                "accessKey": "abc123",
                "secretKey": "abc123",
                "name": "foo",
                "permissions": ["AssetsImport"],
                "systemKey":"false"
            }
            """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClientImpl(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val apikey = client.createApiKey(
            UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"),
            "foo", listOf(Permission.AssetsImport)
        )

        assertEquals(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), apikey.projectId)
        assertEquals(UUID.fromString("11eaad16-d355-4006-90e4-6bd100c3cd81"), apikey.id)
        assertEquals("foo", apikey.name)
        assertEquals(setOf(Permission.AssetsImport), apikey.permissions)
        assertEquals(false, apikey.systemKey)
    }

    @Test
    fun testGetSigningKey() {
        val responseBody =
            """
            {
                "accessKey": "abc123",
                "secretKey": "abc123"
            }
            """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClientImpl(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val apikey = client.getSigningKey(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), "foo")

        assertEquals("abc123", apikey.accessKey)
        assertEquals("abc123", apikey.secretKey)
    }

    @Test
    fun testGetApiKey() {
        val responseBody =
            """
            {
                "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
                "id": "11eaad16-d355-4006-90e4-6bd100c3cd81",
                "accessKey": "abc123",
                "secretKey": "abc123",
                "name": "foo",
                "permissions": ["AssetsImport"]
            }
            """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClientImpl(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val apikey = client.getApiKey(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), "foo")

        assertEquals(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), apikey.projectId)
        assertEquals(UUID.fromString("11eaad16-d355-4006-90e4-6bd100c3cd81"), apikey.id)
        assertEquals("foo", apikey.name)
        assertEquals(setOf(Permission.AssetsImport), apikey.permissions)
    }

    @Test
    fun testAuthToken() {
        val token = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJwcm9qZWN0SWQiOiJDQzdDMkU2Ri0xRTM2LT" +
            "Q3MzEtOTE1NC05NTk4RTIyNDA4QjciLCJrZXlJZCI6IjExRUFBRDE2LUQzNTUtNDAwNi05MEU0LTZCRDEwM" +
            "EMzQ0Q4MSJ9.gH7dTALpd9eTr2TnfPlwu6RqAN-EFPycvHGIFZ-3J1q5GhLCwie0zDSjX4-yTZPYqheDYEsZaE5bYyT6GhfXZg"

        val responseBody =
            """
            {
                "id": "11eaad16-d355-4006-90e4-6bd100c3cd81",
                "projectId": "cc7c2e6f-1e36-4731-9154-9598e22408b7",
                "name": "foo",
                "permissions": ["AssetsImport"]
            }
            """.trimIndent()
        mockServer.enqueue(MockResponse().setBody(responseBody))

        val client = AuthServerClientImpl(mockServer.url("/").toString(), "src/test/resources/signing-key.json")
        val actor = client.authenticate(token)

        assertEquals(UUID.fromString("cc7c2e6f-1e36-4731-9154-9598e22408b7"), actor.projectId)
        assertEquals(UUID.fromString("11eaad16-d355-4006-90e4-6bd100c3cd81"), actor.id)
        assertEquals("foo", actor.name)
        assertEquals(setOf(Permission.AssetsImport), actor.permissions)
    }
}
