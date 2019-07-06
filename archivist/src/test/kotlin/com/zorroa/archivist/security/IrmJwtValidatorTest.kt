package com.zorroa.archivist.security

import com.zorroa.archivist.AbstractTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import java.nio.file.Paths

@SpringBootTest
@RunWith(SpringRunner::class)
@TestPropertySource(locations = ["classpath:test.properties"])
class IrmJwtValidatorTest : AbstractTest() {

    val credsPath = "src/test/resources/test-credentials.json"

    lateinit var token: String

    @Rule
    @JvmField
    final val expectedException = ExpectedException.none()!!

    @Before
    fun init() {
        token = IrmJwtValidatorTest::class.java.getResource("/irm-alice-token.jwt").readText()
    }

    @Test
    fun validate() {
        var validator = IrmJwtValidator(Paths.get(credsPath), mapOf(), userRegistryService)

        var claims = validator.validate(token)
        Assertions.assertThat(claims).isNotNull
        Assertions.assertThat(claims).isNotEmpty
        // User
        Assertions.assertThat(claims["userId"]).isEqualTo("4b33bc4c-ab6f-5df9-a64f-2e39e7f55b27")
        Assertions.assertThat(claims["companyId"]).isEqualTo("25274")
        Assertions.assertThat(claims["insightUser"].orEmpty().isNotBlank())
        Assertions.assertThat(claims["first_name"]).isEqualTo("Alice")
        Assertions.assertThat(claims["last_name"]).isEqualTo("Tester")
        Assertions.assertThat(claims["user_locale"]).isEqualTo("en")
        Assertions.assertThat(claims["mail"]).isEqualTo("Alice_DIT2T@ironmountain.com")
    }
}
