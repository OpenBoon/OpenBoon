package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.InvalidRequestException
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.domain.WebHookUpdate
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class WebHookServiceTests : AbstractTest() {

    @Autowired
    lateinit var webHookService: WebHookService

    lateinit var testHook: WebHook

    @Before
    fun createTestWebHook() {
        val spec = WebHookSpec("https://boonai.app:8081", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        testHook = webHookService.createWebHook(spec)
    }

    @Test
    fun testCreate() {
        val spec = WebHookSpec("http://boonai.app:8080", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        val hook = webHookService.createWebHook(spec)

        assertEquals(spec.url, hook.url)
        assertEquals(spec.secretKey, hook.secretKey)
        assertEquals(spec.triggers, hook.triggers)
    }

    @Test(expected = InvalidRequestException::class)
    fun testCreateLoopbackHookError() {
        val spec = WebHookSpec("http://127.0.0.1:8080", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        webHookService.createWebHook(spec)
    }

    @Test(expected = InvalidRequestException::class)
    fun testCreateSiteLocalHookError() {
        val spec = WebHookSpec("http://192.168.0.1:8080", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        webHookService.createWebHook(spec)
    }

    @Test(expected = InvalidRequestException::class)
    fun testCreateBadUrl() {
        val spec = WebHookSpec("gs://boonai.app", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        webHookService.createWebHook(spec)
    }

    @Test
    fun testGetActive() {
        val hooks = webHookService.getActiveWebHooks()
        assertEquals(1, hooks.size)
    }

    @Test
    fun testFind() {
        val res = webHookService.findWebHooks(WebHookFilter(ids = listOf(testHook.id)))
        assertEquals(1, res.size())
    }

    @Test
    fun testSortedFind() {
        val filt = WebHookFilter(ids = listOf(testHook.id))
        filt.sort = filt.sortMap.keys.map { "$it:a" }
        val res = webHookService.findWebHooks(filt)
        assertEquals(1, res.size())
    }

    @Test(expected = InvalidRequestException::class)
    fun testUpdateEmptyTrigggerFailure() {
        val update = WebHookUpdate("http://boonai.app:9090", "abc", arrayOf(), true)
        webHookService.update(testHook.id, update)
    }

    @Test
    fun testUpdate() {
        val update = WebHookUpdate("http://boonai.app:9090", "abc", arrayOf(TriggerType.AssetModified), true)
        webHookService.update(testHook.id, update)

        val hook = webHookService.findWebHooks(WebHookFilter(ids = listOf(testHook.id)))[0]
        assertEquals("http://boonai.app:9090", hook.url)
        assertEquals("abc", hook.secretKey)
        assertEquals(TriggerType.AssetModified, hook.triggers[0])
    }
}
