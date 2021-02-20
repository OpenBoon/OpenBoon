package boonai.authserver

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import boonai.authserver.domain.ApiKey
import boonai.authserver.domain.ValidationKey
import boonai.authserver.repository.ApiKeyRepository
import boonai.authserver.security.KeyGenerator
import boonai.authserver.service.ApiKeyService
import boonai.common.apikey.Permission
import boonai.common.service.security.EncryptionService
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
    protected lateinit var externalApiKey: ValidationKey

    @MockBean
    lateinit var encryptionService: EncryptionService

    protected lateinit var mockKey: ValidationKey

    protected val mockSecret = "pcekjDV_ipSMXAaBqqtq6Jwy5FAMnjehUQrMEhbG8W01giVqVLfEN9FdMIvzu0rb"

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
            setOf(Permission.AssetsRead.name),
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            "bob/123",
            "bob/123",
            enabled = true,
            systemKey = false,
            hidden = false
        )
        val validationKey = keySpec.getValidationKey()

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                validationKey.getZmlpActor(),
                keySpec.id,
                validationKey.getGrantedAuthorities()
            )
        mockKey = apiKeyRepository.saveAndFlush(keySpec).getValidationKey()
    }

    fun setupMocks() {
        whenever(encryptionService.encryptString(any(), any())).thenReturn(mockSecret)
        whenever(encryptionService.decryptString(any(), any())).thenReturn(mockSecret)
        whenever(encryptionService.encryptString(any(), any(), any())).thenReturn(mockSecret)
        whenever(encryptionService.decryptString(any(), any(), any())).thenReturn(mockSecret)
    }
}
