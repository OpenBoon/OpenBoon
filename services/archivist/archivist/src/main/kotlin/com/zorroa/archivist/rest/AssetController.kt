package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchDeleteAssetResponse
import com.zorroa.archivist.domain.BatchDeleteAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateCustomFieldsRequest
import com.zorroa.archivist.domain.BatchUpdateCustomFieldsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.UpdateAssetLabelsRequest
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.ReprocessAssetSearchResponse
import com.zorroa.archivist.service.AssetSearchService
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.ClipService
import com.zorroa.archivist.service.JobLaunchService
import com.zorroa.archivist.util.RawByteArrayOutputStream
import com.zorroa.archivist.util.RestUtils
import com.zorroa.zmlp.service.logging.LogObject
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.elasticsearch.common.xcontent.ToXContent
import org.elasticsearch.common.xcontent.XContentFactory
import org.slf4j.LoggerFactory
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MultipartFile
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(
    tags = ["Asset"],
    description = "Operations for interacting with Assets including CRUD, streaming, proxies and more."
)
class AssetController @Autowired constructor(
    val assetService: AssetService,
    val assetSearchService: AssetSearchService,
    val jobLaunchService: JobLaunchService,
    val clipService: ClipService
) {

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v3/assets/_search", method = [RequestMethod.GET, RequestMethod.POST])
    fun search(
        @RequestBody(required = false) search: Map<String, Any>?,
        request: WebRequest,
        output: ServletOutputStream
    ): ResponseEntity<Resource> {

        val rsp = assetSearchService.search(search ?: mapOf(), request.parameterMap)
        val output = RawByteArrayOutputStream(1024 * 64)
        XContentFactory.jsonBuilder(output).use {
            rsp.toXContent(it, ToXContent.EMPTY_PARAMS)
        }

        return ResponseEntity.ok()
            .contentLength(output.size().toLong())
            .body(InputStreamResource(output.toInputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @PostMapping("/api/v3/assets/_search/scroll")
    fun scroll(@RequestBody(required = false) scroll: Map<String, String>, output: ServletOutputStream):
        ResponseEntity<Resource> {

            val rsp = assetSearchService.scroll(scroll)
            val output = RawByteArrayOutputStream(1024 * 64)
            XContentFactory.jsonBuilder(output).use {
                rsp.toXContent(it, ToXContent.EMPTY_PARAMS)
            }

            return ResponseEntity.ok()
                .contentLength(output.size().toLong())
                .body(InputStreamResource(output.toInputStream()))
        }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @DeleteMapping("/api/v3/assets/_search/scroll")
    fun clearScroll(@RequestBody(required = false) scroll: Map<String, String>, output: ServletOutputStream):
        ResponseEntity<Resource> {
            val rsp = assetSearchService.clearScroll(scroll)
            val output = RawByteArrayOutputStream(1024 * 1)
            XContentFactory.jsonBuilder(output).use {
                rsp.toXContent(it, ToXContent.EMPTY_PARAMS)
            }

            return ResponseEntity.ok()
                .contentLength(output.size().toLong())
                .body(InputStreamResource(output.toInputStream()))
        }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_search/reprocess")
    fun processSearch(@RequestBody(required = true) req: ReprocessAssetSearchRequest): ReprocessAssetSearchResponse {
        return jobLaunchService.launchJob(req)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping("/api/v3/assets/{id}")
    fun get(@ApiParam("Unique ID of the Asset") @PathVariable id: String): Asset {
        return assetService.getAsset(id)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_batch_create")
    fun batchCreate(@RequestBody request: BatchCreateAssetsRequest):
        BatchCreateAssetsResponse {
            return assetService.batchCreate(request)
        }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PutMapping("/api/v3/assets/_batch_update_custom_fields")
    fun batchUpdateCustomFields(@RequestBody request: BatchUpdateCustomFieldsRequest):
        BatchUpdateCustomFieldsResponse {
            return assetService.batchUpdateCustomFields(request)
        }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @ApiOperation("Create or reprocess assets via a file upload.")
    @PostMapping(value = ["/api/v3/assets/_batch_upload"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun batchUpload(
        @RequestPart(value = "files") files: Array<MultipartFile>,
        @RequestPart(value = "body") req: BatchUploadAssetsRequest
    ): Any {
        req.apply {
            this.files = files
        }
        return assetService.batchUpload(req)
    }

    @ApiOperation("Delete an asset.")
    @PreAuthorize("hasAuthority('AssetsImport')")
    @DeleteMapping(value = ["/api/v3/assets/{id}"])
    @ResponseBody
    fun delete(@PathVariable id: String): Any {
        val rsp = assetService.batchDelete(setOf(id))
        return RestUtils.status("asset", "delete", id, rsp.deleted.contains(id))
    }

    @ApiOperation("Delete assets by query.")
    @PreAuthorize("hasAuthority('AssetsImport')")
    @DeleteMapping(value = ["/api/v3/assets/_batch_delete"])
    @ResponseBody
    fun batchDelete(@RequestBody req: BatchDeleteAssetsRequest): BatchDeleteAssetResponse {
        return assetService.batchDelete(req.assetIds)
    }

    @ApiOperation("Delete assets by query.")
    @PreAuthorize("hasAuthority('AssetsImport')")
    @PutMapping(value = ["/api/v3/assets/_batch_update_labels"])
    @ResponseBody
    fun updateLabels(@RequestBody req: UpdateAssetLabelsRequest): Any {
        val rsp = assetService.updateLabels(req)
        return RestUtils.batchUpdated(
            LogObject.ASSET,
            "_batch_update_labels", rsp.items.count { !it.isFailed }, rsp.items.count { it.isFailed }
        )
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v3/assets/{id}/clips/_search", method = [RequestMethod.GET, RequestMethod.POST])
    fun clipSearch(
        @ApiParam("Unique ID of the Asset") @PathVariable id: String,
        @RequestBody(required = false) search: Map<String, Any>?,
        request: WebRequest,
        output: ServletOutputStream
    ): ResponseEntity<Resource> {

        val asset = assetService.getAsset(id)
        val rsp = clipService.searchClips(asset, search ?: mapOf(), request.parameterMap)
        val output = RawByteArrayOutputStream(1024 * 64)

        XContentFactory.jsonBuilder(output).use {
            rsp.toXContent(it, ToXContent.EMPTY_PARAMS)
        }

        return ResponseEntity.ok()
            .contentLength(output.size().toLong())
            .body(InputStreamResource(output.toInputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v3/assets/{id}/clips/all.vtt", method = [RequestMethod.GET])
    fun getFullWebvtt(
        @ApiParam("Unique ID of the Asset") @PathVariable id: String,
        request: WebRequest,
        response: HttpServletResponse
    ) {
        response.contentType = "text/vtt"
        response.setHeader("Content-Disposition", "attachment; filename=\"all.vtt\"")

        val asset = assetService.getAsset(id)
        clipService.getWebvtt(asset, mapOf(), response.outputStream)
        response.flushBuffer()
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v3/assets/{id}/clips/timelines/{timeline}.vtt", method = [RequestMethod.GET])
    fun getFullWebvttTimeline(
        @ApiParam("Unique ID of the Asset") @PathVariable id: String,
        @ApiParam("Name of the webvtt timeline") @PathVariable timeline: String,
        request: WebRequest,
        response: HttpServletResponse
    ) {
        response.contentType = "text/vtt"
        response.setHeader("Content-Disposition", "attachment; filename=\"$timeline.vtt\"")

        val asset = assetService.getAsset(id)
        clipService.getWebvttByTimeline(asset, timeline, response.outputStream)
        response.flushBuffer()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
