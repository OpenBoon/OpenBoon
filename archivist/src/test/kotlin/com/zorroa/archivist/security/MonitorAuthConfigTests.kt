package com.zorroa.archivist.security

import com.zorroa.archivist.domain.ApiKey
import com.zorroa.archivist.domain.ApiKeySpec
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

    @Autowired
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

        val user = userService.get(username)
        val apiKey = userService.getApiKey(ApiKeySpec(
                user.id, user.username, false, "https://localhost"
        ))
        val token = generateUserToken(user.id, apiKey.key)

        monitorAuthConfig.writeMonitorKeyFile()
        val actual = FileCopyUtils.copyToString(FileReader(keyfile))
        assertThat(actual).isEqualTo(token)
    }

}
