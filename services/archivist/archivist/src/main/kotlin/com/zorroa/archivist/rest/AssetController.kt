package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSearch
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.FileCategory
import com.zorroa.archivist.domain.FileGroup
import com.zorroa.archivist.domain.FileStorageAttrs
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.storage.FileStorageService
import com.zorroa.archivist.util.RawByteArrayOutputStream
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
    val fileStorageService: FileStorageService
) {

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v3/assets/_search", method = [RequestMethod.GET, RequestMethod.POST])
    fun search(@RequestBody(required = false) search: AssetSearch?, output: ServletOutputStream)
        : ResponseEntity<Resource> {

        val rsp = assetService.search(search ?: AssetSearch())
        val output = RawByteArrayOutputStream(1024 * 64)
        XContentFactory.jsonBuilder(output).use {
            rsp.toXContent(it, ToXContent.EMPTY_PARAMS)
        }

        return ResponseEntity.ok()
            .contentLength(output.size().toLong())
            .body(InputStreamResource(output.toInputStream()))
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping("/api/v3/assets/{id}")
    fun get(@ApiParam("Unique ID of the Asset") @PathVariable id: String) : Asset {
        return assetService.getAsset(id)
    }

    @ApiOperation("Stream the source file for the asset is in ZMLP external storage")
    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/assets/{id}/_stream"])
    fun streamAsset(
        @ApiParam("Unique ID of the Asset.") @PathVariable id: String
    ): ResponseEntity<Resource> {
        val asset = assetService.getAsset(id)
        val locator = FileStorageLocator(FileGroup.ASSET, id, FileCategory.SOURCE,
            asset.getAttr("source.filename", String::class.java) as String
        )
        return fileStorageService.stream(locator)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping("/api/v3/assets/_batchCreate")
    fun batchCreate(@RequestBody request: BatchCreateAssetsRequest)
        : BatchCreateAssetsResponse {
        return assetService.batchCreate(request)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PutMapping("/api/v3/assets/_batchUpdate")
    fun batchUpdate(@RequestBody request: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {
        return assetService.batchUpdate(request)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @ApiOperation("Create or reprocess assets via a file upload.")
    @PostMapping(value = ["/api/v3/assets/_batchUpload"], consumes = ["multipart/form-data"])
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
    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping(value = ["/api/v3/assets/{id}/files/{category}"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun uploadFile(
        @PathVariable id: String,
        @PathVariable category: String,
        @RequestPart(value = "file") file: MultipartFile,
        @RequestPart(value = "body") req: FileStorageAttrs
    ): Any {
        val asset = assetService.getAsset(id)
        val locator = FileStorageLocator(FileGroup.ASSET, asset.id,
            FileCategory.valueOf(category.toUpperCase()), req.name)
        val spec = FileStorageSpec(locator, req.attrs, file.bytes)
        return fileStorageService.store(spec)
    }

    @ApiOperation("Store an additional file to an asset.")
    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/assets/{id}/files/{category}/{name}"])
    @ResponseBody
    fun streamFile(
        @PathVariable id: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): ResponseEntity<Resource> {
        val locator = FileStorageLocator(FileGroup.ASSET, id,
            FileCategory.valueOf(category.toUpperCase()), name)
        return fileStorageService.stream(locator)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
