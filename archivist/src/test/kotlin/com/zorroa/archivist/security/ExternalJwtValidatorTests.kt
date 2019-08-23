package com.zorroa.archivist.security

import com.zorroa.archivist.AbstractTest
import org.elasticsearch.index.query.QueryStringQueryBuilder
import org.elasticsearch.index.query.SimpleQueryStringBuilder
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
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
    fun testEmbeddedQueryStringFilter() {
        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1NjY1MDE1MDUsImV4cCI6MTU5ODAzNzUwNSwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsInVzZXJJZCI6IjAwMDAwMDAwLTdiMGItNDgwZS04YzM2LWYwNmYwNGFlZDJmMSIsIm9yZ2FuaXphdGlvbklkIjoiMDAwMDAwMDAtOTk5OC04ODg4LTc3NzctNjY2NjY2NjY2NjY2IiwicXVlcnRTdHJpbmdGaWx0ZXIiOiJzb3VyY2UudHlwZTppbWFnZSJ9.4hGTBU1RHST-pojdDoGyAZGUe0QUN2xTiDfPrqYhnpA"
        val payload = """{
            "iss": "Online JWT Builder",
            "iat": 1566501505,
            "exp": 1598037505,
            "aud": "www.example.com",
            "sub": "jrocket@example.com",
            "userId": "00000000-7b0b-480e-8c36-f06f04aed2f1",
            "organizationId": "00000000-9998-8888-7777-666666666666",
            "queryStringFilter": "source.type:image"
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

        // Take our validated claims and wrap them in a JwtAuthenticationToken
        val vjwt = JwtAuthenticationToken(validator.validate(token))
        // Call authenticate to run the JwtAuthenticationProvider clsass
        val auth = authenticationManager.authenticate(vjwt)
        // Replaces the globally logged in user with our authenticated user.
        SecurityContextHolder.getContext().authentication = auth

        // Get a permission filter.
        val filter = getAssetPermissionsFilter(null)
        assertTrue(filter is QueryStringQueryBuilder)
        assertEquals("source.type:image", filter.queryString())
    }

    @Test
    fun testEmbeddedSearchFilter() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE1NjYzMzkwNjAsImV4cCI6MTU5Nzg3NTA2MCwiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsInVzZXJJZCI6IiAwMDAwMDAwMC03YjBiLTQ4MGUtOGMzNi1mMDZmMDRhZWQyZjEiLCJvcmdhbml6YXRpb25JZCI6IjAwMDAwMDAwLTk5OTgtODg4OC03Nzc3LTY2NjY2NjY2NjY2NiIsImZpbHRlciI6IntcInF1ZXJ5XCI6IHsgXCJ0ZXJtc1wiOiB7IFwic291cmNlLnR5cGVcIjogW1widmlkZW9cIl0gfSB9IH0ifQ.alnldvNVPDSclcyFAk-PrhEw791KUr_mEVL-ZnwIMAw"
        val payload = """{
                "iss": "Online JWT Builder",
                "iat": 1566339060,
                "exp": 1597875060,
                "aud": "www.example.com",
                "sub": "jrocket@example.com",
                "userId": "00000000-7b0b-480e-8c36-f06f04aed2f1",
                "organizationId": "00000000-9998-8888-7777-666666666666",
                "filter": "{\"query\": { \"terms\": { \"source.type\": [\"video\"] } } }"
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

        // Take our validated claims and wrap them in a JwtAuthenticationToken
        val vjwt = JwtAuthenticationToken(validator.validate(token))
        // Call authenticate to run the JwtAuthenticationProvider clsass
        val auth = authenticationManager.authenticate(vjwt)
        // Replaces the globally logged in user with our authenticated user.
        SecurityContextHolder.getContext().authentication = auth

        // Get a permission filter.
        val filter = getAssetPermissionsFilter(null).toString()
        val knownFilter = """{
  "terms" : {
    "source.type" : [
      "video"
    ],
    "boost" : 1.0
  }
}""".trimIndent()
        assertEquals(knownFilter, filter)
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