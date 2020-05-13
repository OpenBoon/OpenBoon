package com.zorroa.auth.server.service

import com.zorroa.auth.server.AbstractTest
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.domain.ProjectApiKeysEnabledSpec
import com.zorroa.auth.server.security.getProjectId
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.apikey.ZmlpActor
import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun testApiKeyUpdateEnabled() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead),
            true
        )
        var key = apiKeyService.create(spec)
        assertEquals(true, key.enabled)
        apiKeyService.updateEnabled(key, false)
        val newKey = apiKeyService.get(key.id)
        assertEquals(false, newKey.enabled)
    }

    @Test
    fun testProjectApiKeysUpdateEnabled() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead),
            true
        )

        var key = apiKeyService.create(spec)
        apiKeyService.updateEnabledByProject(ProjectApiKeysEnabledSpec(false))

        val findAll = apiKeyService.findAll()
        assertEquals(false, findAll[0].enabled)
        assertEquals(false, findAll[1].enabled)
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

    @Test
    fun testSearchProjectIdFilter() {
        val pid = UUID.randomUUID()
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead),
            projectId = pid
        )

        apiKeyService.create(spec)
        val keys = apiKeyService.search(ApiKeyFilter(names = listOf("test")))

        // No keys should be found.
        assertEquals(0, keys.list.size)
    }
    @Test
    fun testSearchByPrefix() {

        apiKeyService.create(ApiKeySpec("test1", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("test2", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("test3", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("try1", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("try2", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("try3", setOf(Permission.AssetsRead)))

        val keys = apiKeyService.search(ApiKeyFilter(namePrefixes = listOf("test", "try")))

        assertEquals(6, keys.list.size)
    }

    @Test
    fun testSearchByPrefixAndName() {

        apiKeyService.create(ApiKeySpec("test1", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("test2", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("test3", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("try1", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("try2", setOf(Permission.AssetsRead)))
        apiKeyService.create(ApiKeySpec("try3", setOf(Permission.AssetsRead)))

        val keys = apiKeyService.search(
            ApiKeyFilter(
                namePrefixes = listOf("test"),
                names = listOf("test1")
            )
        )

        assertEquals(1, keys.list.size)
    }

    @Test
    fun testSearchByNameAndId() {

        val create1 = apiKeyService.create(ApiKeySpec("test1", setOf(Permission.AssetsRead)))
        val create2 = apiKeyService.create(ApiKeySpec("try1", setOf(Permission.AssetsRead)))

        val keys = apiKeyService.search(
            ApiKeyFilter(
                ids = listOf(create1.id),
                names = listOf(create2.name)
            )
        )

        assertEquals(0, keys.list.size)
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
