package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetFileLocator
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageRequest
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.ReprocessAssetSearchRequest
import com.zorroa.archivist.domain.ReprocessAssetSearchResponse
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.domain.UpdateAssetsByQueryRequest
import com.zorroa.archivist.service.AssetSearchService
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.JobLaunchService
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.RawByteArrayOutputStream
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.apache.http.util.EntityUtils
import org.elasticsearch.common.Strings
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

@RestController
@Timed
@Api(
    tags = ["Asset"],
    description = "Operations for interacting with Assets including CRUD, streaming, proxies and more."
)
class AssetController @Autowired constructor(
    val assetService: AssetService,
    val assetSearchService: AssetSearchService,
    val projectStorageService: ProjectStorageService,
    val jobLaunchService: JobLaunchService
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
    fun clear_scroll(@RequestBody(required = false) scroll: Map<String, String>, output: ServletOutputStream):
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
    @PostMapping("/api/v3/assets/{id}/_update")
    fun update(
        @ApiParam("Unique ID of the Asset") @PathVariable id: String,
        @RequestBody(required = true) update: UpdateAssetRequest
    ): ResponseEntity<Resource> {
        val bytes = EntityUtils.toByteArray(assetService.update(id, update).entity)
        return ResponseEntity.ok()
            .contentLength(bytes.size.toLong())
            .body(InputStreamResource(bytes.inputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_update_by_query")
    fun updateByQuery(
        @RequestBody(required = true) update: UpdateAssetsByQueryRequest
    ): ResponseEntity<Resource> {
        val bytes = EntityUtils.toByteArray(assetService.updateByQuery(update).entity)
        return ResponseEntity.ok()
            .contentLength(bytes.size.toLong())
            .body(InputStreamResource(bytes.inputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_batch_update")
    fun batchUpdate(
        @RequestBody(required = true) batch: Map<String, UpdateAssetRequest>
    ): ResponseEntity<Resource> {
        val rsp = assetService.batchUpdate(batch)
        val content = Strings.toString(rsp)
        return ResponseEntity.ok()
            .contentLength(content.length.toLong())
            .body(InputStreamResource(content.byteInputStream()))
    }

    @ApiOperation("Stream the source file for the asset is in ZMLP external storage")
    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/assets/{id}/_stream"])
    fun streamAsset(
        @ApiParam("Unique ID of the Asset.") @PathVariable id: String
    ): ResponseEntity<Resource> {
        val asset = assetService.getAsset(id)
        val locator = AssetFileLocator(
            id, ProjectStorageCategory.SOURCE,
            asset.getAttr("source.filename", String::class.java) as String
        )
        return projectStorageService.stream(locator)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_batch_create")
    fun batchCreate(@RequestBody request: BatchCreateAssetsRequest):
        BatchCreateAssetsResponse {
        return assetService.batchCreate(request)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PutMapping("/api/v3/assets/{id}/_index")
    fun index(
        @ApiParam("Unique ID of the Asset.") @PathVariable id: String,
        @RequestBody doc: MutableMap<String, Any>
    ): Any {
        val bytes = EntityUtils.toByteArray(assetService.index(id, doc).entity)
        return ResponseEntity.ok()
            .contentLength(bytes.size.toLong())
            .body(InputStreamResource(bytes.inputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_batch_index")
    fun batchIndex(@RequestBody req: Map<String, MutableMap<String, Any>>): ResponseEntity<Resource> {
        val rsp = assetService.batchIndex(req)
        val content = Strings.toString(rsp)
        return ResponseEntity.ok()
            .contentLength(content.length.toLong())
            .body(InputStreamResource(content.byteInputStream()))
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

    @ApiOperation("Store an additional file to an asset.")
    // Only job runner keys can store files.
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PostMapping(value = ["/api/v3/assets/{id}/_files"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun uploadFile(
        @PathVariable id: String,
        @RequestPart(value = "file") file: MultipartFile,
        @RequestPart(value = "body") req: ProjectStorageRequest
    ): Any {
        val asset = assetService.getAsset(id)
        val locator = AssetFileLocator(asset.id, req.category, req.name)
        val spec = ProjectStorageSpec(locator, req.attrs, file.bytes)
        return projectStorageService.store(spec)
    }

    @ApiOperation("Stream a file associated with asset.")
    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/assets/{id}/_files/{category}/{name}"])
    @ResponseBody
    fun streamFile(
        @PathVariable id: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): ResponseEntity<Resource> {
        val locator = AssetFileLocator(id, category, name)
        return projectStorageService.stream(locator)
    }

    @ApiOperation("Get get underlying file location.", hidden = true)
    // Only job runners can get this.
    @PreAuthorize("hasAuthority('SystemProjectDecrypt')")
    @GetMapping(value = ["/api/v3/assets/{id}/_locate/{category}/{name}"])
    @ResponseBody
    fun getBlobLocation(
        @PathVariable id: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): Any {
        val locator = AssetFileLocator(id, category, name)
        return mapOf("uri" to projectStorageService.getNativeUri(locator))
    }

    @ApiOperation("Delete an asset.")
    @PreAuthorize("hasAuthority('AssetsImport')")
    @DeleteMapping(value = ["/api/v3/assets/{id}"])
    @ResponseBody
    fun delete(@PathVariable id: String): ResponseEntity<Resource> {
        val rsp = assetService.delete(id)
        val bytes = EntityUtils.toByteArray(rsp.entity)
        return ResponseEntity.ok()
            .contentLength(bytes.size.toLong())
            .body(InputStreamResource(bytes.inputStream()))
    }

    @ApiOperation("Delete assets by query.")
    @PreAuthorize("hasAuthority('AssetsImport')")
    @DeleteMapping(value = ["/api/v3/assets/_delete_by_query"])
    @ResponseBody
    fun deleteByQuery(@RequestBody req: Map<String, Any>): ResponseEntity<Resource> {
        val rsp = assetService.deleteByQuery(req)
        val content = Strings.toString(rsp)
        return ResponseEntity.ok()
            .contentLength(content.length.toLong())
            .body(InputStreamResource(content.byteInputStream()))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
