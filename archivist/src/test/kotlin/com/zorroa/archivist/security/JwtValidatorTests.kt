package com.zorroa.archivist.security

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
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

class IrmJwtValidatorTest {

    lateinit var token1: String
    lateinit var token2: String

    @Before
    fun setup() {
        token1 = IrmJwtValidatorTest::class.java.getResource("/irm-qa-tester-token.jwt").readText()
        token2 = IrmJwtValidatorTest::class.java.getResource("/irm-imconnect-company-provision.jwt")
            .readText()
    }

    @Test
    fun validate() {
        var validator = IrmJwtValidator()
        var claims1 = validator.validate(token1)
        assertThat(claims1).isNotNull
        assertThat(claims1).isNotEmpty
        assertThat(claims1["userId"]).isEqualTo("IRMUSER_QA_TESTER")
        assertThat(claims1["companyId"]).isEqualTo("0")
        assertThat(claims1["insightUser"].orEmpty().isNotBlank())

        var claims2 = validator.validate(token2)
        assertThat(claims2).isNotNull
        assertThat(claims2).isNotEmpty
        assertThat(claims2["userId"]).isEqualTo("IRMUSER_IMCONNECT_COMPANY_PROVISION")
        assertThat(claims2).containsEntry("companyId", "0")
        assertThat(claims2["insightUser"].orEmpty().isNotBlank())
    }
}
