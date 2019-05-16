package com.zorroa.archivist.rest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.BatchDeleteAssetsRequest
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
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
    meterRegistry: MeterRegistry) {

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

    @GetMapping(value = ["/api/v1/assets/_fields"])
    fun getFields(response: HttpServletResponse) : Map<String, Set<String>> {
        response.setHeader("Cache-Control", CacheControl.maxAge(
                30, TimeUnit.SECONDS).cachePrivate().headerValue)
        return fieldService.getFields("asset")
    }

    val mapping: Map<String, Any>
        @GetMapping(value = ["/api/v1/assets/_mapping"])
        @Throws(IOException::class)
        get() = indexService.getMapping()

    /**
     * Handle a HEAD request which a client can use to fetch a singed URL for
     * a file in bucket storage, such as GCS or S3.
     *
     * The Accept header should be used to specify media types that the requesting
     * application can display.  For example if the application can display EXR
     * files, it should send "image/x-exr" in the accept header.
     *
     * @param id The asset ID
     * @param headers The request headers.
     * @param rsp the HTTP response.
     *
     */
    @RequestMapping(value = ["/api/v1/assets/{id}/_stream"], method = [RequestMethod.HEAD])
    fun streamAsset(
        @PathVariable id: String,
        @RequestHeader headers: HttpHeaders,
        rsp: HttpServletResponse) {

        val servableFile = assetStreamResolutionService.getServableFile(id, headers.accept)
        if (servableFile == null) {
            rsp.status = 404
        } else {
            if (!servableFile.isLocal()) {
                rsp.setHeader("X-Zorroa-Signed-URL", servableFile.getSignedUrl().toString())
            }
        }
    }

    /**
     * Stream the best possible representation for the asset.
     *
     * The ext parameter can be used to short circuit the content negotiation logic
     * and ask for a specific file extension.
     *
     * The Accept header should be used to specify media types that the requesting
     * application can display.  For example if the application can display EXR
     * files, it should send "image/x-exr" in the accept header.
     *
     * @param id The asset Id.
     * @param ext An optional file extension to serve.
     * @param headers The HTTP request headers.
     * @param req The HTTP request.
     * @param rsp The HTTP response.
     *
     */
    @GetMapping(value = ["/api/v1/assets/{id}/_stream"])
    fun streamAsset(
        @PathVariable id: String,
        @RequestParam(value = "ext", required = false) ext: String?,
        @RequestHeader headers: HttpHeaders,
        req: HttpServletRequest,
        rsp: HttpServletResponse) {

        try {
            /**
             * Handle converting the ext query param to a media type, otherwise
             * default to accept headers.
             */
            val mediaTypes = if (ext != null) {
                listOf(MediaType.parseMediaType(StaticUtils.tika.detect(".$ext")))
            }
            else {
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
            }
            else {
                logger.warn("Failed to stream asset ID $id, with media types $mediaTypes")
                rsp.status = 404
            }
        } catch (e: Exception) {
            logger.warn("Failed to stream asset ID $id", e)
            rsp.status = 404
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/closest/{width:\\d+}x{height:\\d+}"])
    @Throws(IOException::class)
    fun getClosestProxy(req: HttpServletRequest,
                        rsp: HttpServletResponse,
                        @PathVariable id: String,
                        @PathVariable width: Int,
                        @PathVariable height: Int,
                        @RequestParam(value="type", defaultValue = "image") type: String)  {
        return try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).getClosest(width, height, type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/atLeast/{size:\\d+}"])
    @Throws(IOException::class)
    fun getAtLeast(req: HttpServletRequest,
                   rsp: HttpServletResponse,
                   @PathVariable id: String,
                   @PathVariable(required = true) size: Int,
                   @RequestParam(value="type", defaultValue = "image") type: String) {
        try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).atLeastThisSize(size, type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/largest"])
    @Throws(IOException::class)
    fun getLargestProxy(req: HttpServletRequest,
                        rsp: HttpServletResponse,
                        @PathVariable id: String,
                        @RequestParam(value="type", defaultValue = "image") type: String) {
        try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).getLargest(type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/smallest"])
    @Throws(IOException::class)
    fun getSmallestProxy(req: HttpServletRequest,
                         rsp: HttpServletResponse,
                         @PathVariable id: String,
                         @RequestParam(value="type", defaultValue = "image") type: String) {
        return try {
            imageService.serveImage(rsp, proxyLookupCache.get(id).getSmallest(type))
        } catch (e: Exception) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
    }

    @PostMapping(value = ["/api/v3/assets/_search"])
    @Throws(IOException::class)
    fun searchV3(@RequestBody search: AssetSearch): PagedList<Document> {
        return searchService.search(Pager(search.from, search.size, 0), search)
    }

    @PostMapping(value = ["/api/v4/assets/_search"])
    @Throws(IOException::class)
    fun searchV4(@RequestBody search: AssetSearch, out: ServletOutputStream) {
        searchService.search(Pager(search.from, search.size, 0), search, out)
    }

    @PostMapping(value = ["/api/v2/assets/_count"])
    @Throws(IOException::class)
    fun count(@RequestBody search: AssetSearch): Any {
        return HttpUtils.count(searchService.count(search))
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_exists"])
    @Throws(IOException::class)
    fun exists(@PathVariable id: String): Any {
        return HttpUtils.exists(id, indexService.exists(id))
    }

    @PostMapping(value = ["/api/v3/assets/_suggest"])
    @Throws(IOException::class)
    fun suggestV3(@RequestBody suggest: AssetSuggestBuilder): Any {
        return searchService.getSuggestTerms(suggest.text)
    }

    @GetMapping(value = ["/api/v2/assets/{id}", "/api/v1/assets/{id}"])
    fun get(@PathVariable id: String): Any {
        return indexService.get(id)
    }

    @GetMapping(value = ["/api/v1/assets/{id}/fieldSets"])
    fun getFieldSets(@PathVariable id: String): List<FieldSet> {
        return assetService.getFieldSets(id)
    }

    @GetMapping(value = ["/api/v1/assets/_path"])
    fun getByPath(@RequestBody path: Map<String, String>): Document? {
        return path["path"]?.let { indexService.get(Paths.get(it)) }
    }

    @DeleteMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    fun delete(@PathVariable id: String): Any {
        return HttpUtils.deleted("asset", id, assetService.delete(id))
    }

    @DeleteMapping(value = ["/api/v1/assets"])
    @Throws(IOException::class)
    fun batchDelete(@RequestBody batch: BatchDeleteAssetsRequest): BatchDeleteAssetsResponse {
        return assetService.batchDelete(batch.assetIds)
    }

    @PutMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    fun update(@RequestBody attrs: Map<String, Any>, @PathVariable id: String): Any {
        val rsp = assetService.updateAssets(
                BatchUpdateAssetsRequest(mapOf(id to UpdateAssetRequest(attrs))))
        if (rsp.isSuccess()) {
            return HttpUtils.updated("asset", id, true, assetService.get(id))
        }
        else {
            throw rsp.getThrowableError()
        }
    }

    @PutMapping(value = ["/api/v2/assets/{id}"])
    @Throws(IOException::class)
    fun updateV2(@PathVariable id: String, @RequestBody req: UpdateAssetRequest): Any {
        val rsp = assetService.updateAssets(
                BatchUpdateAssetsRequest(mapOf(id to req)))
        if (rsp.isSuccess()) {
            return HttpUtils.updated("asset", id, true, assetService.get(id))
        }
        else {
            throw rsp.getThrowableError()
        }
    }

    @PutMapping(value = ["/api/v1/assets"])
    fun batchUpdate(@RequestBody req: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {
        return assetService.updateAssets(req)
    }

    @PostMapping(value = ["/api/v1/assets/_index"])
    @Throws(IOException::class)
    fun batchCreate(@RequestBody spec: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        return assetService.createOrReplaceAssets(spec)
    }

    class SetFoldersRequest {
        var folders: List<UUID>? = null
    }

    /**
     * Reset all folders for a given asset.  Currently only used for syncing.
     */
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/assets/{id}/_setFolders"])
    @Throws(Exception::class)
    fun setFolders(@PathVariable id: String, @RequestBody req: SetFoldersRequest): Any {
        req?.folders?.let {
            folderService.setFoldersForAsset(id, it)
            return HttpUtils.updated("asset", id, true)
        }
        return HttpUtils.updated("asset", id, false)
    }

    @PutMapping(value = ["/api/v2/assets/_permissions"])
    @Throws(Exception::class)
    fun setPermissionsV2(@RequestBody req: BatchUpdatePermissionsRequest) : BatchUpdatePermissionsResponse {
        return assetService.setPermissions(req)
    }

    @PostMapping(value = ["/api/v1/assets/_upload", "/api/v1/imports/_upload"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun upload(@RequestParam("files") files: Array<MultipartFile>,
               @RequestParam("body") body: String): Any {
        val spec = Json.deserialize(body, FileUploadSpec::class.java)
        return fileUploadService.ingest(spec, files)
    }

    @PutMapping(value = ["/api/v1/refresh"])
    fun refresh() {
        logger.warn("Refresh called.")
    }

    @GetMapping(value = ["/api/v1/assets/{id}/fieldEdits"])
    fun getFieldEdits(@PathVariable id: UUID): List<FieldEdit>  {
        return fieldSystemService.getFieldEdits(id)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
