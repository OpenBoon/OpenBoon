package com.zorroa.archivist.security

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import org.junit.Ignore
import org.junit.Test
import java.io.FileInputStream

class JwtValidatorTests {

    val credsPath = "src/test/resources/test-credentials.json"

    @Test
    @Ignore
    fun testConstruct() {
        val creds = GoogleCredential.fromStream(FileInputStream(credsPath))
        val validator = GcpJwtValidator(creds)
    }
}
