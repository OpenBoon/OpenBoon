package com.zorroa.auth.server

import com.zorroa.auth.client.Permission
import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.repository.ApiKeyRepository
import com.zorroa.auth.server.security.KeyGenerator
import com.zorroa.auth.server.service.ApiKeyService
import java.util.UUID
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@Transactional
@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractTest {

    @Autowired
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    protected lateinit var apiKeyService: ApiKeyService

    @Autowired
    protected lateinit var externalApiKey: ApiKey

    protected lateinit var standardKey: ApiKey

    @Before
    fun setup() {
        // A standard non-admin for testing.
        val keySpec = ApiKey(
            UUID.randomUUID(),
            UUID.randomUUID(),
            KeyGenerator.generate(24),
            KeyGenerator.generate(32),
            "standard-key",
            setOf(Permission.AssetsRead.name)
        )

        standardKey = apiKeyRepository.save(keySpec)

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                standardKey.getZmlpActor(),
                standardKey.id,
                standardKey.getGrantedAuthorities()
            )
    }
}
