package com.zorroa.auth

import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.service.ApiKeyService
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractTest {

    @Autowired
    protected lateinit var apiKeyService: ApiKeyService

    @Autowired
    protected lateinit var externalApiKey: ApiKey

    protected lateinit var standardKey: ApiKey

    @Before
    fun setup() {
        // A tandard non-admin for testing.
        standardKey = apiKeyService.create(
                ApiKeySpec("standard-key",
                        UUID.randomUUID(),
                        listOf("Test")))

        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                standardKey.projectId, standardKey.keyId, standardKey.getGrantedAuthorities())
    }

}