package com.zorroa.archivist.rest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSuggestBuilder
import com.zorroa.archivist.service.*
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.Json
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Paths
import java.util.*
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


    @RequestMapping(value = ["/api/v1/assets/{id}/_stream"], method = [RequestMethod.HEAD] )
    @Throws(Exception::class)
    fun streamAsset(@RequestParam(defaultValue = "true", required = false) fallback: Boolean,
                    @RequestHeader(value="Accept", required = false) accept: String?,
                    @PathVariable id: String, response: HttpServletResponse) {

        val servableFile = assetStreamResolutionService.getServableFile(id, accept)
        if (servableFile == null) {
            response.status = 404
        } else {
            if (!servableFile.isLocal()) {
                response.setHeader("X-Zorroa-Signed-URL", servableFile.getSignedUrl().toString())
            }
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_stream"])
    @Throws(Exception::class)
    fun streamAsset(@RequestParam(defaultValue = "true", required = false) fallback: Boolean,
                    @RequestParam(value = "type", required = false) type: String?,
                    @RequestHeader(value="Accept", required = false) accept: String?,
                    @PathVariable id: String, request: HttpServletRequest, response: HttpServletResponse) {

        try {
            val servableFile = assetStreamResolutionService.getServableFile(id, accept, type)
            if (servableFile == null) {
                response.status = 404
            } else {
                logger.event(LogObject.ASSET, LogAction.STREAM, mapOf("assetId" to id))
                if (!servableFile.isLocal()) {
                    servableFile.copyTo(response)
                } else {
                    MultipartFileSender.fromPath(servableFile.getLocalFile())
                        .with(request)
                        .with(response)
                        .setContentType(servableFile.getStat().mediaType)
                        .serveResource()
                }
            }
        } catch (e: Exception) {
            response.sendError(404, "StorageSystem unable to find file")
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
            imageService.serveImage(req, rsp, proxyLookupCache.get(id)!!.getClosest(width, height, type))
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
            imageService.serveImage(req, rsp, proxyLookupCache.get(id).atLeastThisSize(size, type))
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
            imageService.serveImage(req, rsp, proxyLookupCache.get(id).getLargest(type))
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
            imageService.serveImage(req, rsp, proxyLookupCache.get(id).getSmallest(type))
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
