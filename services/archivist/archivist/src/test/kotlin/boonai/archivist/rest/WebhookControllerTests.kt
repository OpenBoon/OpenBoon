package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.TriggerType
import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookPatch
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.domain.WebHookUpdate
import boonai.archivist.service.WebHookService
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class WebhookControllerTests : MockMvcTest() {

    @Autowired
    lateinit var webHookService: WebHookService

    lateinit var webhook: WebHook

    @Before
    fun initialize() {
        val spec = WebHookSpec("http://boonai.app", "abc123", arrayOf(TriggerType.ASSET_DELETED))
        webhook = webHookService.createWebHook(spec)
    }

    @Test
    fun testCreate() {
        val testSpec = WebHookSpec("http://boonai.app", "abc123", arrayOf(TriggerType.ASSET_DELETED))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/webhooks")
                .headers(admin())
                .content(Json.serialize(testSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.url", CoreMatchers.equalTo(testSpec.url)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.secretKey", CoreMatchers.equalTo(testSpec.secretKey)))
            .andReturn()
    }

    @Test
    fun testDelete() {
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/webhooks/${webhook.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testPatch() {
        val patch = WebHookPatch(url = "https://api.boonai.io:5000", secretKey = "dog")

        mvc.perform(
            MockMvcRequestBuilders.patch("/api/v3/webhooks/${webhook.id}")
                .headers(admin())
                .content(Json.serialize(patch))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val update = WebHookUpdate(
            "https://foobar:8000", webhook.secretKey, webhook.triggers, webhook.active
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/webhooks/${webhook.id}")
                .headers(admin())
                .content(Json.serialize(update))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", CoreMatchers.equalTo(true)))
            .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/webhooks/${webhook.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(webhook.id.toString())))
            .andReturn()
    }

    @Test
    fun testSearch() {
        val req = mapOf("ids" to listOf(webhook.id))
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/webhooks/_search")
                .headers(admin())
                .content(Json.serialize(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.size", CoreMatchers.equalTo(1)))
            .andReturn()
    }
}
