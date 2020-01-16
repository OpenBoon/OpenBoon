package com.zorroa.auth.server

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.repository.ApiKeyRepository
import com.zorroa.auth.server.security.KeyGenerator
import com.zorroa.auth.server.service.ApiKeyService
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.service.security.EncryptionService
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("test", "aws")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class AbstractTest {

    @Autowired
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    protected lateinit var apiKeyService: ApiKeyService

    @Autowired
    protected lateinit var externalApiKey: ApiKey

    @MockBean
    lateinit var encryptionService: EncryptionService

    protected lateinit var standardKey: ApiKey

    @Before
    fun setup() {
        setupApiKey()
        setupMocks()
    }

    fun setupApiKey() {

        // A standard non-admin for testing.
        val keySpec = ApiKey(
            UUID.randomUUID(),
            UUID.randomUUID(),
            KeyGenerator.generate(24),
            KeyGenerator.generate(32),
            "standard-key",
            setOf(Permission.AssetsRead.name)
        )

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                keySpec.getZmlpActor(),
                keySpec.id,
                keySpec.getGrantedAuthorities()
            )

        standardKey = apiKeyRepository.save(keySpec)
    }

    fun setupMocks() {
        whenever(
            encryptionService.encryptString(any(), any())
        ).thenReturn("pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb")
        whenever(
            encryptionService.decryptString(any(), any())
        ).thenReturn("pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb")
    }
}
