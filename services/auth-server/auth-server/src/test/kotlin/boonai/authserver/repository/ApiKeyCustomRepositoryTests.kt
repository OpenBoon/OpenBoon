package boonai.authserver.repository

import boonai.authserver.AbstractTest
import boonai.authserver.domain.ApiKeySpec
import boonai.common.apikey.Permission
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataRetrievalFailureException
import kotlin.test.assertEquals

class ApiKeyCustomRepositoryTests : AbstractTest() {

    @Autowired
    lateinit var apiKeyCustomRepository: ApiKeyCustomRepository

    @Test
    fun testGetSigningKeyById() {
        val key = apiKeyCustomRepository.getSigningKey(mockKey.id)
        assertEquals(mockSecret, key.secretKey)
        assertEquals(mockKey.accessKey, key.accessKey)
    }

    @Test
    fun testGetSigningKey() {
        val key = apiKeyCustomRepository.getSigningKey(mockKey.name)
        assertEquals(mockSecret, key.secretKey)
        assertEquals(mockKey.accessKey, key.accessKey)
    }

    @Test
    fun testGetValidationKey() {
        val key = apiKeyCustomRepository.getValidationKey(mockKey.accessKey)
        assertEquals(mockKey.accessKey, key.accessKey)
        assertEquals(mockKey.id, key.id)
        assertEquals(mockKey.name, key.name)
        assertEquals(mockKey.permissions, key.permissions)
        assertEquals(mockKey.projectId, key.projectId)
        assertEquals(mockKey.secretKey, key.secretKey)
    }

    @Test(expected = DataRetrievalFailureException::class)
    fun testGetValidationKeyFail() {

        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val create = apiKeyService.create(spec)
        val key = apiKeyCustomRepository.getValidationKey(create.accessKey)

        assertEquals(create.id, key.id)

        // Should fail when Enabled = false
        apiKeyService.updateEnabled(create, false)
        val keyException = apiKeyCustomRepository.getValidationKey(create.accessKey)
    }
}
