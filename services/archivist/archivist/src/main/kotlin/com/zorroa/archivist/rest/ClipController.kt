package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.BatchUpdateClipProxyRequest
import com.zorroa.archivist.domain.Clip
import com.zorroa.archivist.domain.UpdateClipProxyRequest
import com.zorroa.archivist.domain.ClipSpec
import com.zorroa.archivist.domain.CreateTimelineResponse
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.domain.WebVTTFilter
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.ClipService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.util.RawByteArrayOutputStream
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(tags = ["Timeline"], description = "Operations for interacting with Timelines")
class ClipController @Autowired constructor(
    val assetService: AssetService,
    val clipService: ClipService
) {

    @PreAuthorize("hasAuthority('AssetsImport')")
    @RequestMapping("/api/v1/clips/_timeline", method = [RequestMethod.POST])
    fun createWithTimeline(
        @RequestBody(required = true) timeline: TimelineSpec
    ): CreateTimelineResponse {
        return clipService.createClips(timeline)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @RequestMapping("/api/v1/clips", method = [RequestMethod.POST])
    fun create(
        @RequestBody(required = true) spec: ClipSpec
    ): Clip {
        return clipService.createClip(spec)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @PostMapping("/api/v1/clips/_search")
    fun search(
        @RequestBody(required = false) search: Map<String, Any>?,
        request: WebRequest,
        output: ServletOutputStream
    ): ResponseEntity<Resource> {

        val rsp = clipService.searchClips(null, search ?: mapOf(), request.parameterMap)
        val output = RawByteArrayOutputStream(1024 * 64)

        XContentFactory.jsonBuilder(output).use {
            rsp.toXContent(it, ToXContent.EMPTY_PARAMS)
        }

        return ResponseEntity.ok()
            .contentLength(output.size().toLong())
            .body(InputStreamResource(output.toInputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v1/clips/_webvtt", method = [RequestMethod.GET, RequestMethod.POST])
    fun search(
        @RequestBody(required = true) filter: WebVTTFilter,
        request: WebRequest,
        response: HttpServletResponse
    ) {

        response.contentType = "text/vtt"
        response.setHeader("Content-Disposition", "attachment; filename=\"zvi-dynamic.vtt\"")
        clipService.streamWebvtt(filter, response.outputStream)
        response.flushBuffer()
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PutMapping("/api/v1/clips/_batch_update_proxy")
    fun batchUpdateProxy(
        @RequestBody(required = true) req: BatchUpdateClipProxyRequest
    ): Any {
        return clipService.batchSetProxy(req)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PutMapping("/api/v1/clips/{id}/_proxy")
    fun setProxy(
        @PathVariable id: String,
        @RequestBody(required = true) proxy: UpdateClipProxyRequest
    ): Any {
        return HttpUtils.updated("clip", id, clipService.setProxy(id, proxy))
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @DeleteMapping("/api/v1/clips/{id}")
    fun deleteClip(
        @PathVariable id: String
    ): Any {
        return HttpUtils.deleted("clip", id, clipService.deleteClip(id))
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping("/api/v1/clips/{id}")
    fun getClip(
        @PathVariable id: String
    ): Clip {
        return clipService.getClip(id)
    }
}
