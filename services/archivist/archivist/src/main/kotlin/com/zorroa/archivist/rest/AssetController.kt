package com.zorroa.archivist.rest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.schema.ProxySchema
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.ImageService
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@Api(
    tags = ["Asset"],
    description = "Operations for interacting with Assets including CRUD, streaming, proxies and more."
)
class AssetController @Autowired constructor(
    private val assetService: AssetService,
    private val imageService: ImageService,
    private val fileServerProvider: FileServerProvider,
    meterRegistry: MeterRegistry
) {

    private val proxyLookupCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .concurrencyLevel(10)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(object : CacheLoader<String, ProxySchema>() {
            @Throws(Exception::class)
            override fun load(id: String): ProxySchema {
                return assetService.getProxies(id)
            }
        })

    init {
        meterRegistry.gauge("zorroa.cache.proxy-cache-size", proxyLookupCache) {
            it.size().toDouble()
        }
    }

    @ApiOperation("Stream the source file for the asset if available")
    @GetMapping(value = ["/api/v1/assets/{id}/_stream"])
    fun streamAsset(
        @ApiParam("Unique ID of the Asset.") @PathVariable id: String
    ): ResponseEntity<Resource> {
        val asset = assetService.get(id)
        val storage = fileServerProvider.getServableFile(asset)
        val resource = InputStreamResource(storage.getInputStream())

        val fileSize = asset.getAttr("source.fileSize", Long::class.java)
        return if (fileSize == null) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.ok()
                .contentLength(fileSize)
                .contentType(MediaType.parseMediaType(asset.getAttr("source.mimetype")))
                .body(resource)
        }
    }

    @ApiOperation(
        "Returns the proxy file closest in size.",
        notes = "Based on the resolution set in the url the image proxy that is closest in size will be returned."
    )
    @GetMapping(value = ["/api/v1/assets/{id}/proxies/closest/{width:\\d+}x{height:\\d+}"])
    @Throws(IOException::class)
    fun getClosestProxy(
        req: HttpServletRequest,
        rsp: HttpServletResponse,
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("Width (in pixels) for the resolution to try matching.") @PathVariable width: Int,
        @ApiParam("Height (in pixels) for the resolution to try matching.") @PathVariable height: Int,
        @ApiParam("Type of proxy to return.", allowableValues = "image,video")
        @RequestParam(value = "type", defaultValue = "image") type: String
    ) {
        return try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).getClosest(width, height, type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @ApiOperation(
        "Return a proxy file this size or larger.",
        notes = "Returns a proxy whose width or height (in pixels) is at least this size."
    )
    @GetMapping(value = ["/api/v1/assets/{id}/proxies/atLeast/{size:\\d+}"])
    @Throws(IOException::class)
    fun getAtLeast(
        req: HttpServletRequest,
        rsp: HttpServletResponse,
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("Length (in pixels) to use as a miniumum for proxy size.")
        @PathVariable(required = true) size: Int,
        @ApiParam("Type of proxy to return.", allowableValues = "image,video")
        @RequestParam(value = "type", defaultValue = "image") type: String
    ) {
        try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).atLeastThisSize(size, type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @ApiOperation("Returns the largest proxy file.")
    @GetMapping(value = ["/api/v1/assets/{id}/proxies/largest"])
    @Throws(IOException::class)
    fun getLargestProxy(
        req: HttpServletRequest,
        rsp: HttpServletResponse,
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("Type of proxy to return.", allowableValues = "image,video")
        @RequestParam(value = "type", defaultValue = "image") type: String
    ) {
        try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).getLargest(type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @ApiOperation("Returns the smallest proxy file.")
    @GetMapping(value = ["/api/v1/assets/{id}/proxies/smallest"])
    @Throws(IOException::class)
    fun getSmallestProxy(
        req: HttpServletRequest,
        rsp: HttpServletResponse,
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("Type of proxy to return.", allowableValues = "image,video")
        @RequestParam(value = "type", defaultValue = "image") type: String
    ) {
        return try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).getSmallest(type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @RequestMapping("/api/v3/assets/_search", method = [RequestMethod.GET, RequestMethod.POST])
    fun search(@RequestBody(required = false) query: Map<String, Any>?, out: ServletOutputStream) {
        assetService.search(query ?: mapOf(), out)
    }

    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'AssetsRead')")
    @GetMapping("/api/v3/assets/{id}")
    fun get(@ApiParam("Unique ID of the Asset") @PathVariable id: String) : Asset {
        return assetService.get(id)
    }

    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'AssetsWrite')")
    @PostMapping("/api/v3/assets/_batchCreate")
    fun batchCreate(@RequestBody request: BatchCreateAssetsRequest)
        : BatchCreateAssetsResponse {
        return assetService.batchCreate(request)
    }

    @PreAuthorize("hasAnyAuthority('ProjectAdmin', 'AssetsWrite')")
    @PutMapping("/api/v3/assets/_batchUpdate")
    fun batchUpdate(@RequestBody request: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {
        return assetService.batchUpdate(request)
    }

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

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
