package com.zorroa.auth.server.repository

import com.zorroa.auth.server.AbstractTest
import kotlin.test.assertEquals
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

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
}
