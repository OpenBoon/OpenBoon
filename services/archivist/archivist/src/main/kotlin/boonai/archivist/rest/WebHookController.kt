package boonai.archivist.rest

import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookPatch
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.domain.WebHookUpdate
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.WebHookPublisherService
import boonai.archivist.service.WebHookService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('AssetsImport')")
@RestController
class WebHookController constructor(
    val webHookService: WebHookService,
    val webHookPublisherService: WebHookPublisherService
) {

    @PostMapping(value = ["/api/v3/webhooks"])
    fun create(@RequestBody spec: WebHookSpec): WebHook {
        return webHookService.createWebHook(spec)
    }

    @DeleteMapping(value = ["/api/v3/webhooks/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        webHookService.deleteWebHook(webHookService.getWebHook(id))
        return HttpUtils.deleted("WebHook", id, true)
    }

    @GetMapping(value = ["/api/v3/webhooks/{id}"])
    fun get(@PathVariable id: UUID): WebHook {
        return webHookService.getWebHook(id)
    }

    @PostMapping(value = ["/api/v3/webhooks/{id}/_test"])
    fun test(@PathVariable id: UUID): Any {
        val wb = webHookService.getWebHook(id)
        webHookPublisherService.testWebHook(wb)
        return HttpUtils.status("webhook", "_test", true)
    }

    @PostMapping(value = ["/api/v3/webhooks/_test"])
    fun craftTest(@RequestBody spec: WebHookSpec): Any {
        webHookPublisherService.testWebHook(spec)
        return HttpUtils.status("webhook", "_test", true)
    }

    @PutMapping(value = ["/api/v3/webhooks/{id}"])
    fun update(@PathVariable id: UUID, @RequestBody spec: WebHookUpdate): Any {
        val updated = webHookService.update(id, spec)
        return HttpUtils.updated("WebHook", id, updated)
    }

    @PatchMapping(value = ["/api/v3/webhooks/{id}"])
    fun patch(@PathVariable id: UUID, @RequestBody spec: WebHookPatch): Any {
        val hook = webHookService.getWebHook(id)
        val update = WebHookUpdate(
            spec.url ?: hook.url,
            spec.secretKey ?: hook.secretKey,
            spec.triggers ?: hook.triggers,
            spec.active ?: hook.active
        )
        val updated = webHookService.update(hook.id, update)
        return HttpUtils.updated("WebHook", id, updated)
    }

    @ApiOperation("Search for Webhooks.")
    @PostMapping("/api/v3/webhooks/_search")
    fun find(@RequestBody(required = false) filter: WebHookFilter?): KPagedList<WebHook> {
        return webHookService.findWebHooks(filter ?: WebHookFilter())
    }

    @ApiOperation("Find a single WebHook")
    @PostMapping("/api/v3/webhooks/_find_one")
    fun findOne(@RequestBody(required = false) filter: WebHookFilter?): WebHook {
        return webHookService.findOneWebHook(filter ?: WebHookFilter())
    }
}
