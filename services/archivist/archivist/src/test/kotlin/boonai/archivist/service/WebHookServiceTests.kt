package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookSpec
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
        val spec = WebHookSpec("https://localhost:8081", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        testHook = webHookService.createWebHook(spec)
    }

    @Test
    fun testCreate() {
        val spec = WebHookSpec("http://localhost:8080", "abc123", arrayOf(TriggerType.AssetAnalyzed))
        val hook = webHookService.createWebHook(spec)

        assertEquals(spec.url, hook.url)
        assertEquals(spec.secretToken, hook.secretToken)
        assertEquals(spec.triggers, hook.triggers)
    }

    @Test
    fun testGetActive() {
        val hooks = webHookService.getActiveWebHooks()
        assertEquals(1, hooks.size)
    }
}
