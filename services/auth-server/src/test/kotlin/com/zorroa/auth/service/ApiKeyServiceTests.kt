package com.zorroa.auth.service

import com.zorroa.auth.AbstractTest
import com.zorroa.auth.domain.ApiKeyFilter
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.security.getProjectId
import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiKeyServiceTests : AbstractTest() {

    @Test
    fun testCreate() {
        val spec = ApiKeySpec(
            "test",
            UUID.randomUUID(),
            listOf("foo")
        )
        val key = apiKeyService.create(spec)
        assertEquals(spec.name, key.name)
        assertEquals(spec.projectId, key.projectId)
        assertTrue("foo" in key.permissions)
    }

    @Test
    fun testGet() {
        val spec = ApiKeySpec(
            "test",
            getProjectId(),
            listOf("foo")
        )
        val key1 = apiKeyService.create(spec)
        val key2 = apiKeyService.get(key1.keyId)
        assertEquals(key1, key2)
    }

    @Test
    fun testFindOne() {
        val spec = ApiKeySpec(
            "test",
            getProjectId(),
            listOf("foo")
        )
        val key1 = apiKeyService.create(spec)
        val key2 = apiKeyService.findOne(ApiKeyFilter(names = listOf("test")))
        assertEquals(key1, key2)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testFindOneFailure() {
        apiKeyService.findOne(ApiKeyFilter(names = listOf("mrcatman")))
    }

    @Test
    fun testFindAll() {
        val spec = ApiKeySpec(
            "test",
            getProjectId(),
            listOf("foo")
        )
        val key1 = apiKeyService.create(spec)
        val all = apiKeyService.findAll()
        assertTrue(key1 in all)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testDelete() {
        val spec = ApiKeySpec(
            "test",
            getProjectId(),
            listOf("foo")
        )
        val key1 = apiKeyService.create(spec)
        apiKeyService.delete(key1)
        apiKeyService.get(key1.keyId)
    }
}