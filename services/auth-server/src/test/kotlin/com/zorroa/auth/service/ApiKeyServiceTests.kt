package com.zorroa.auth.service

import AbstractTest
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.domain.Role
import com.zorroa.auth.security.getProjectId
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import java.util.*
import kotlin.test.assertTrue

class ApiKeyServiceTests : AbstractTest() {

    @Test
    fun testCreate() {
        val spec = ApiKeySpec(
                "test",
                UUID.randomUUID(),
                listOf(Role.USER_ROLE)
        )
        val key = apiKeyService.create(spec)
        assertEquals(spec.name, key.name)
        assertEquals(spec.projectId, key.projectId)
        assertTrue(Role.USER_ROLE in key.permissions)
    }

    @Test
    fun testGet() {
        val spec = ApiKeySpec(
                "test",
                getProjectId(),
                listOf(Role.USER_ROLE)
        )
        val key1 = apiKeyService.create(spec)
        val key2 = apiKeyService.get(key1.keyId)
        assertEquals(key1, key2)
    }

    @Test
    fun testFindAll() {
        val spec = ApiKeySpec(
                "test",
                getProjectId(),
                listOf(Role.USER_ROLE)
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
                listOf(Role.USER_ROLE)
        )
        val key1 = apiKeyService.create(spec)
        apiKeyService.delete(key1)
        apiKeyService.get(key1.keyId)
    }
}