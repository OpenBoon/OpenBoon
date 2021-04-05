package boonai.archivist.rest

import boonai.archivist.domain.WebHook
import boonai.archivist.domain.WebHookFilter
import boonai.archivist.domain.WebHookSpec
import boonai.archivist.domain.WebHookUpdate
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.WebHookService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

class WebHookController constructor(val webHookService: WebHookService) {

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping(value = ["/api/v3/webhooks"])
    fun create(@RequestBody spec: WebHookSpec): WebHook {
        return webHookService.createWebHook(spec)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @DeleteMapping(value = ["/api/v3/webhooks/{id}"])
    fun remove(@PathVariable id: UUID): Any {
        webHookService.deleteWebHook(webHookService.getWebHook(id))
        return HttpUtils.deleted("WebHook", id, true)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/webhooks/{id}"])
    fun get(@PathVariable id: UUID): WebHook {
        return webHookService.getWebHook(id)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/webhooks/{id}"])
    fun update(@PathVariable id: UUID, @RequestBody spec: WebHookUpdate): Any {
        val updated = webHookService.update(id, spec)
        return HttpUtils.updated("WebHook", id, updated)
    }

    @ApiOperation("Search for Webhooks.")
    @PostMapping("/api/v3/webhooks/_search")
    fun find(@RequestBody(required = false) filter: WebHookFilter?): KPagedList<WebHook> {
        return webHookService.findWebHooks(filter ?: WebHookFilter())
    }

    @ApiOperation("Find a single Field")
    @PostMapping("/api/v3/webhooks/_find_one")
    fun findOne(@RequestBody(required = false) filter: WebHookFilter?): WebHook {
        return webHookService.findOneWebHook(filter ?: WebHookFilter())
    }
}
