package com.zorroa.auth.server.service

import com.zorroa.auth.server.AbstractTest
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.security.getProjectId
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.apikey.ZmlpActor
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class ApiKeyServiceTests : AbstractTest() {

    @Test
    fun testCreateOverrideProject() {

        val actor = ZmlpActor(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "bigAdmin",
            setOf(Permission.SystemProjectOverride)
        )

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                actor,
                actor.id,
                setOf()
            )

        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val key = apiKeyService.create(spec)
        assertEquals(spec.name, key.name)
        assertEquals(getProjectId(), key.projectId)
        assertTrue(Permission.AssetsRead.name in key.permissions)
    }

    @Test
    fun testGet() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val key1 = apiKeyService.create(spec)
        val key2 = apiKeyService.get(key1.id)
        assertEquals(key1, key2)
    }

    @Test
    fun testFindOne() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val key1 = apiKeyService.create(spec)
        val key2 = apiKeyService.findOne(ApiKeyFilter(names = listOf("test")))
        assertEquals(key1, key2)
    }

    @Test
    fun testSearch() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val key1 = apiKeyService.create(spec)
        val keys = apiKeyService.search(ApiKeyFilter(names = listOf("test")))

        assertEquals(1, keys.list.size)
        assertEquals("test", keys.list[0].name)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testFindOneFailure() {
        apiKeyService.findOne(ApiKeyFilter(names = listOf("mrcatman")))
    }

    @Test
    fun testFindAll() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val key1 = apiKeyService.create(spec)
        val all = apiKeyService.findAll()
        assertTrue(key1 in all)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testDelete() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val key1 = apiKeyService.create(spec)
        apiKeyService.delete(key1)
        apiKeyService.get(key1.id)
    }
}
