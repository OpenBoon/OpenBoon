package com.zorroa.common.clients

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import org.apache.http.client.methods.HttpPost
import org.junit.Test
import java.io.FileInputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GcpJwtSignerTests {

    val credsPath = "src/test/resources/test-credentials.json"

    @Test(expected = IllegalStateException::class)
    fun testSign() {
        val signer = GcpJwtSigner()
    }

    @Test
    fun testSignWithKeyFromPath() {
        val signer = GcpJwtSigner(credsPath)
        val req = HttpPost("https://foo")
        signer.sign(req, "https://foo")
        assertNotNull(req.getHeaders("Authorization"))
        assertTrue(req.getHeaders("Authorization")[0].toString()
                .startsWith("Authorization: Bearer "))
    }

    @Test
    fun testSignWithCreds() {
        val creds = GoogleCredential.fromStream(FileInputStream(credsPath))
        val signer = GcpJwtSigner(creds)
        val req = HttpPost("https://foo")
        signer.sign(req, "https://foo")
        assertNotNull(req.getHeaders("Authorization"))
        assertTrue(req.getHeaders("Authorization")[0].toString()
                .startsWith("Authorization: Bearer "))
    }
}
