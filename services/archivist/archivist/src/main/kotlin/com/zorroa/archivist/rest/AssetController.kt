package com.zorroa.archivist.rest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.schema.ProxySchema
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.AssetStreamResolutionService
import com.zorroa.archivist.service.ImageService
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.StaticUtils
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
    private val assetStreamResolutionService: AssetStreamResolutionService,
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
    
    @ApiOperation(
        "Handle a HEAD request which a client can use to fetch a singed URL for the asset.",
        notes = "The signed url is a fqdn that has authentication built in and can be used by a browser to retrieve the asset " +
            "from a bucket storage location such as GCS or S3. The Accept header should be used to specify media types " +
            "that the requesting application can display.  For example if the application can display EXR files, it " +
            "should send \"image/x-exr\" in the accept header."
    )
    @RequestMapping(value = ["/api/v1/assets/{id}/_stream"], method = [RequestMethod.HEAD])
    fun streamAsset(
        @ApiParam("UUID of the asset") @PathVariable id: String,
        @RequestHeader headers: HttpHeaders,
        rsp: HttpServletResponse
    ) {

        val servableFile = assetStreamResolutionService.getServableFile(id, headers.accept)
        if (servableFile == null) {
            rsp.status = 404
        } else {
            if (!servableFile.isLocal()) {
                rsp.setHeader("X-Zorroa-Signed-URL", servableFile.getSignedUrl().toString())
            }
        }
    }

    @ApiOperation(
        "Stream the best possible representation for the asset.",
        notes = "The ext parameter can be used to short circuit the content negotiation logic and ask for a specific " +
            "file extension. The Accept header should be used to specify media types that the requesting application " +
            "can display. For example if the application can display EXR files, it should send \"image/x-exr\" in " +
            "the accept header."
    )
    @GetMapping(value = ["/api/v1/assets/{id}/_stream"])
    fun streamAsset(
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("An optional file extension to serve.") @RequestParam(value = "ext", required = false) ext: String?,
        @RequestHeader headers: HttpHeaders,
        req: HttpServletRequest,
        rsp: HttpServletResponse
    ) {

        try {
            /**
             * Handle converting the ext query param to a media type, otherwise
             * default to accept headers.
             */
            val mediaTypes = if (ext != null) {
                listOf(MediaType.parseMediaType(StaticUtils.tika.detect(".$ext")))
            } else {
                headers.accept
            }

            val servableFile = assetStreamResolutionService.getServableFile(id, mediaTypes)
            if (servableFile != null) {

                logger.event(LogObject.ASSET, LogAction.STREAM, mapOf("assetId" to id, "url" to servableFile.uri))

                if (!servableFile.isLocal()) {
                    servableFile.copyTo(rsp)
                } else {
                    MultipartFileSender.fromPath(servableFile.getLocalFile())
                        .with(req)
                        .with(rsp)
                        .setContentType(servableFile.getStat().mediaType)
                        .serveResource()
                }
            } else {
                logger.warn("Failed to stream asset ID $id, with media types $mediaTypes")
                rsp.status = 404
            }
        } catch (e: Exception) {
            if (logger.isDebugEnabled) {
                logger.debug("Interrupted while streaming $id", e)
            } else {
                logger.warn("Interrupted while streaming Asset $id")
            }

            rsp.status = 404
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

    @RequestMapping("/assets/_search", method = [RequestMethod.GET, RequestMethod.POST])
    fun search(@RequestBody query: Map<String, Any>, out: ServletOutputStream) {
        assetService.search(query, out)
    }

    @PreAuthorize("hasAnyAuthority('AssetsImport', 'ProjectAdmin', 'AssetsWrite')")
    @PostMapping("/api/v3/assets/_batchCreate")
    fun batchCreate(@RequestBody request: BatchCreateAssetsRequest)
        : BatchCreateAssetsResponse {
        return assetService.batchCreate(request)
    }

    @PreAuthorize("hasAnyAuthority('AssetsImport', 'ProjectAdmin', 'AssetsWrite')")
    @PutMapping("/api/v3/assets/_batchUpdate")
    fun batchUpdate(@RequestBody request: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {
        return assetService.batchUpdate(request)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
