package com.zorroa.archivist.security

import com.zorroa.archivist.domain.ApiKey
import com.zorroa.archivist.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.util.FileCopyUtils
import java.io.FileReader
import java.util.*

@SpringBootTest
@RunWith(SpringRunner::class)
@TestPropertySource(locations = ["classpath:test.properties"])
class MonitorAuthConfigTests {

    @MockBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var appContext: ConfigurableApplicationContext

    @Autowired
    private lateinit var monitorAuthConfig: MonitorAuthConfig

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    @Throws(Exception::class)
    fun writeMonitorAuthKey() {

        val keyfile = "${folder.root.path}/monitor.auth.key"
        val username = "monitor"

        TestPropertyValues.of(
            "archivist.monitor.key.path=$keyfile",
            "archivist.monitor.username=$username"
        ).applyTo(appContext)

        val uuid = UUID.fromString("7ec62c0a-e6eb-4141-bf8c-12876c445bff")

        val hmacKey = "some_hmac_key"

        given(userService.getApiKey(username)).willReturn(ApiKey(uuid, hmacKey))

        monitorAuthConfig.writeMonitorKeyFile()

        val token = generateUserToken(ApiKey(uuid, hmacKey))
        val actual = FileCopyUtils.copyToString(FileReader(keyfile))

        assertThat(actual).isEqualTo(token)
    }

}
