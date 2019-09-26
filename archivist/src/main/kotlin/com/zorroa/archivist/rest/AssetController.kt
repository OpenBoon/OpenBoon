package com.zorroa.archivist.rest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchDeleteAssetsRequest
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.BatchIndexAssetsResponse
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetsResponse
import com.zorroa.archivist.domain.BatchUpdatePermissionsRequest
import com.zorroa.archivist.domain.BatchUpdatePermissionsResponse
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.domain.FieldSet
import com.zorroa.archivist.domain.FileUploadSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSuggestBuilder
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.AssetStreamResolutionService
import com.zorroa.archivist.service.FieldService
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.archivist.service.FileUploadService
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.ImageService
import com.zorroa.archivist.service.IndexService
import com.zorroa.archivist.service.SearchService
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.util.StaticUtils
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.Json
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import io.swagger.annotations.Api
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(
    tags = ["Asset"],
    description = "Operations for interacting with Assets including CRUD, streaming, proxies and more."
)
class AssetController @Autowired constructor(
    private val indexService: IndexService,
    private val assetService: AssetService,
    private val searchService: SearchService,
    private val folderService: FolderService,
    private val imageService: ImageService,
    private val assetStreamResolutionService: AssetStreamResolutionService,
    private val fieldService: FieldService,
    private val fileUploadService: FileUploadService,
    private val fieldSystemService: FieldSystemService,
    meterRegistry: MeterRegistry
) {

    private val proxyLookupCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .concurrencyLevel(10)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(object : CacheLoader<String, ProxySchema>() {
            @Throws(Exception::class)
            override fun load(id: String): ProxySchema {
                return indexService.getProxies(id)
            }
        })

    init {
        meterRegistry.gauge("zorroa.cache.proxy-cache-size", proxyLookupCache) {
            it.size().toDouble()
        }
    }

    @ApiOperation("Gets a list of all metadata fields an Asset could have.")
    @GetMapping(value = ["/api/v1/assets/_fields"])
    fun getFields(response: HttpServletResponse): Map<String, Set<String>> {
        response.setHeader(
            "Cache-Control", CacheControl.maxAge(
                30, TimeUnit.SECONDS
            ).cachePrivate().headerValue
        )
        return fieldService.getFields("asset")
    }

    val mapping: Map<String, Any>
        @GetMapping(value = ["/api/v1/assets/_mapping"])
        @Throws(IOException::class)
        get() = indexService.getMapping()

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

    @ApiOperation(
        "Search for Assets.",
        notes = "Returns a list of Assets that matched the given search filter."
    )
    @PostMapping(value = ["/api/v3/assets/_search"])
    @Throws(IOException::class)
    fun searchV3(
        @ApiParam("Filter to use for searching for Assets.")
        @RequestBody search: AssetSearch
    ): PagedList<Document> {
        return searchService.search(Pager(search.from, search.size, 0), search)
    }

    @ApiOperation(
        "Search for Assets.",
        notes = "Returns a list of Assets that matched the given search filter."
    )
    @PostMapping(value = ["/api/v4/assets/_search"])
    @Throws(IOException::class)
    fun searchV4(
        @ApiParam("Filter to use for searching for Assets.") @RequestBody search: AssetSearch,
        out: ServletOutputStream
    ) {
        searchService.search(Pager(search.from, search.size, 0), search, out)
    }

    @ApiOperation("Delete a search scroll by id.")
    @DeleteMapping(value = ["/api/v1/assets/_scroll"])
    @Throws(IOException::class)
    fun clearScroll(
        @ApiParam("Request body containing a scroll_id property, mimics ES API.") @RequestBody req: Map<String, String>
    ): Any {
        require("scroll_id" in req) { "Request body does not contain 'scroll_id'" }
        return HttpUtils.status("asset", "clearScroll", searchService.clearScroll(req.getValue("scroll_id")))
    }

    @ApiOperation("Returns number of Assets matching the search given.")
    @PostMapping(value = ["/api/v2/assets/_count"])
    @Throws(IOException::class)
    fun count(@ApiParam("Filter to use for searching for Assets.") @RequestBody search: AssetSearch): Any {
        return HttpUtils.count(searchService.count(search))
    }

    @ApiOperation("Checks if an Assets exists.")
    @GetMapping(value = ["/api/v1/assets/{id}/_exists"])
    @Throws(IOException::class)
    fun exists(@ApiParam("UUID of the Asset.") @PathVariable id: String): Any {
        return HttpUtils.exists(id, indexService.exists(id))
    }

    @ApiOperation(
        "Get a list of keyword suggestions.",
        notes = "Intended to help auto-populate a search bar with suggestions as a user types."
    )
    @PostMapping(value = ["/api/v3/assets/_suggest"])
    @Throws(IOException::class)
    fun suggestV3(
        @ApiParam(
            "Suggestion builder that allows for adding an asset search filter and a text " +
                "filter. The most common usage is to just add the text (i.e. {\"text\": \"ca\"})."
        )
        @RequestBody suggest: AssetSuggestBuilder
    ): Any {
        return searchService.getSuggestTerms(suggest.text)
    }

    @ApiOperation("Get an Asset.")
    @GetMapping(value = ["/api/v2/assets/{id}", "/api/v1/assets/{id}"])
    fun get(@ApiParam("UUID of the Asset.") @PathVariable id: String): Any {
        return indexService.get(id)
    }

    @ApiOperation("Get Field Sets for an Asset.")
    @GetMapping(value = ["/api/v1/assets/{id}/fieldSets"])
    fun getFieldSets(@ApiParam("UUID of the Asset.") @PathVariable id: String): List<FieldSet> {
        return assetService.getFieldSets(id)
    }

    @ApiOperation(
        "Get Assets that match a source path.",
        notes = "Returns any Assets whose source.path metadata matches the path sent in the request body."
    )
    @GetMapping(value = ["/api/v1/assets/_path"])
    fun getByPath(@ApiParam("Path to get Assets for.") @RequestBody path: Map<String, String>): Document? {
        return path["path"]?.let { indexService.get(Paths.get(it)) }
    }

    @ApiOperation("Delete multiple Assets.")
    @DeleteMapping(value = ["/api/v1/assets"])
    @Throws(IOException::class)
    fun batchDelete(
        @ApiParam("Assets to delete.") @RequestBody batch: BatchDeleteAssetsRequest
    ): BatchDeleteAssetsResponse {
        return assetService.batchDelete(batch.assetIds)
    }

    @ApiOperation("Delete an Asset.")
    @DeleteMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    fun delete(
        @ApiParam("UUID of the Asset.") @PathVariable id: String
    ): Any {
        return HttpUtils.deleted("asset", id, assetService.delete(id))
    }

    @ApiOperation("Update an Asset.")
    @PutMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    fun update(
        @ApiParam("Attributes to update.") @RequestBody attrs: Map<String, Any>,
        @ApiParam("UUID of the Asset.") @PathVariable id: String
    ): Any {
        val rsp = assetService.updateAssets(
            BatchUpdateAssetsRequest(mapOf(id to UpdateAssetRequest(attrs)))
        )
        if (rsp.isSuccess()) {
            return HttpUtils.updated("asset", id, true, assetService.get(id))
        } else {
            throw rsp.getThrowableError()
        }
    }

    @ApiOperation("Update an Asset.")
    @PutMapping(value = ["/api/v2/assets/{id}"])
    @Throws(IOException::class)
    fun updateV2(
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("Updates to make to the Asset.") @RequestBody req: UpdateAssetRequest
    ): Any {
        val rsp = assetService.updateAssets(
            BatchUpdateAssetsRequest(mapOf(id to req))
        )
        if (rsp.isSuccess()) {
            return HttpUtils.updated("asset", id, true, assetService.get(id))
        } else {
            throw rsp.getThrowableError()
        }
    }

    @ApiOperation("Update multiple Assets.")
    @PutMapping(value = ["/api/v1/assets"])
    fun batchUpdate(
        @ApiParam("List of Asset updates.") @RequestBody req: BatchUpdateAssetsRequest
    ): BatchUpdateAssetsResponse {
        return assetService.updateAssets(req)
    }

    @ApiOperation("Create multiple Assets.")
    @PostMapping(value = ["/api/v1/assets/_index"])
    @Throws(IOException::class)
    fun batchCreate(
        @ApiParam("Assets to create.") @RequestBody spec: BatchCreateAssetsRequest
    ): BatchIndexAssetsResponse {
        return assetService.createOrReplaceAssets(spec)
    }

    @ApiModel("Set Folders Request")
    class SetFoldersRequest {
        @ApiModelProperty("UUIDs of Folders to set.")
        var folders: List<UUID>? = null
    }

    @ApiOperation("Reset all folders for a given asset. Currently only used for syncing.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/assets/{id}/_setFolders"])
    @Throws(Exception::class)
    fun setFolders(
        @ApiParam("UUID of the Asset.") @PathVariable id: String,
        @ApiParam("Folders to reset.") @RequestBody req: SetFoldersRequest
    ): Any {
        req?.folders?.let {
            assetService.batchSetLinks(id, it)
            return HttpUtils.updated("asset", id, true)
        }
        return HttpUtils.updated("asset", id, false)
    }

    @ApiOperation("Update the permissions for multiple Assets.")
    @PutMapping(value = ["/api/v2/assets/_permissions"])
    @Throws(Exception::class)
    fun setPermissionsV2(
        @ApiParam("List of permission updates.") @RequestBody req: BatchUpdatePermissionsRequest
    ): BatchUpdatePermissionsResponse {
        return assetService.setPermissions(req)
    }

    @ApiOperation("Create a new asset from an uploaded file.")
    @PostMapping(value = ["/api/v1/assets/_upload", "/api/v1/imports/_upload"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun upload(
        @RequestParam("files") files: Array<MultipartFile>,
        @RequestParam("body") body: String
    ): Any {
        val spec = Json.deserialize(body, FileUploadSpec::class.java)
        return fileUploadService.ingest(spec, files)
    }

    @ApiOperation("DEPRECATED: Exists only for backwards compatibility.")
    @PutMapping(value = ["/api/v1/refresh"])
    fun refresh() {
        logger.warn("Refresh called.")
    }

    @ApiOperation("Return a list of edits made to this Asset's fields.")
    @GetMapping(value = ["/api/v1/assets/{id}/fieldEdits"])
    fun getFieldEdits(@ApiParam("UUID of the Asset.") @PathVariable id: UUID): List<FieldEdit> {
        return fieldSystemService.getFieldEdits(id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
