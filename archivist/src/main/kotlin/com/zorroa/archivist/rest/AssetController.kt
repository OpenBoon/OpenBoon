package com.zorroa.archivist.rest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSuggestBuilder
import com.zorroa.archivist.security.canExport
import com.zorroa.archivist.service.*
import com.zorroa.archivist.util.event
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.net.URI
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class AssetController @Autowired constructor(
        private val indexService: IndexService,
        private val assetService: AssetService,
        private val searchService: SearchService,
        private val folderService: FolderService,
        private val imageService: ImageService,
        private val fieldService: FieldService,
        private val fileServerProvider: FileServerProvider,
        private val fileStorageService: FileStorageService
){

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


    fun getPreferredFormat(asset: Document, forceProxy: Boolean): ServableFile? {
        if (forceProxy) {
            val proxy = getProxyStream(asset)
             if (proxy != null) {
                 return fileServerProvider.getServableFile(URI(proxy.uri))
             }

        } else  {
            return fileServerProvider.getServableFile(fileServerProvider.getStorageUri(asset))
        }

        return null
    }

    fun getProxyStream(asset: Document): FileStorage? {
        // If the file doesn't have a proxy this will throw.
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)

        if (proxies != null) {
            val largest = proxies.getLargest()
            if (largest != null) {
                fileStorageService.get(largest.id!!)
            }
        }
        return null
    }

    @RequestMapping(value = ["/api/v1/assets/{id}/_stream"], method = [RequestMethod.GET, RequestMethod.HEAD])
    @Throws(Exception::class)
    fun streamAsset(@RequestParam(defaultValue = "true", required = false) fallback: Boolean,
                    @RequestParam(value = "ext", required = false) ext: String?,
                    @PathVariable id: String, request: HttpServletRequest, response: HttpServletResponse) {

        val asset = indexService.get(id)
        val canExport = canExport(asset)
        val ofile = getPreferredFormat(asset, !canExport)

        if (ofile == null) {
            response.status = 404
        }
        else {
            if (request.method == "HEAD") {
                /**
                 * Only non-local files need to be signed.
                 */
                if (!ofile.isLocal()) {
                    response.setHeader("X-Zorroa-Signed-URL", ofile.getSignedUrl().toString())
                }
            }
            else {
                try {
                    logger.event("view Asset", mapOf("assetId" to asset.id))
                    if (!ofile.isLocal()) {
                        ofile.copyTo(response)
                    } else {
                        MultipartFileSender.fromPath(ofile.getLocalFile())
                                .with(request)
                                .with(response)
                                .setContentType(ofile.getStat().mediaType)
                                .serveResource()

                    }
                } catch (e: Exception) {
                    response.sendError(404, "StorageSystem unable to find file")
                }
            }
        }
    }

    private val proxyLookupCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(object : CacheLoader<String, Proxy>() {
                @Throws(Exception::class)
                override fun load(slug: String): Proxy? {
                    val e = slug.split(":")
                    val proxies = indexService.getProxies(e[1])

                    return when {
                        e[0] == "closest" -> proxies.getClosest(e[2].toInt(), e[3].toInt(), e.last())
                        e[0] == "atLeast" -> proxies.atLeastThisSize(e[2].toInt(), e.last())
                        e[0] == "smallest" -> proxies.getSmallest(e.last())
                        e[0] == "largest" -> proxies.getLargest(e.last())
                        else -> proxies.getLargest(e.last())
                    }
                }
            })

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/closest/{width:\\d+}x{height:\\d+}"])
    @Throws(IOException::class)
    fun getClosestProxy(req: HttpServletRequest,
                        rsp: HttpServletResponse,
                        @PathVariable id: String,
                        @PathVariable width: Int,
                        @PathVariable height: Int,
                        @RequestParam(value="type", defaultValue = "image") type: String)  {
        return try {
            imageService.serveImage(req, rsp, proxyLookupCache.get("closest:$id:$width:$height:$type"))
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
            imageService.serveImage(req, rsp, proxyLookupCache.get("atLeast:$id:$size:$type"))
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
            imageService.serveImage(req, rsp, proxyLookupCache.get("largest:$id:$type"))
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
            imageService.serveImage(req, rsp, proxyLookupCache.get("smallest:$id:$type"))
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

    @PutMapping(value = ["/api/v1/assets/_fields/hide"])
    @Throws(IOException::class)
    fun unhideField(@RequestBody update: HideField): Any {
        return HttpUtils.status("field", "hide",
                fieldService.updateField(update.setHide(true).setManual(true)))
    }

    @DeleteMapping(value = ["/api/v1/assets/_fields/hide"])
    @Throws(IOException::class)
    fun hideField(@RequestBody update: HideField): Any {
        return HttpUtils.status("field", "unhide",
                fieldService.updateField(update.setHide(false)))
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

    @GetMapping(value = ["/api/v2/assets/{id}"])
    fun get(@PathVariable id: String): Any {
        return indexService.get(id)
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

    @PutMapping(value = ["/api/v1/assets/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(IOException::class)
    fun update(@RequestBody attrs: Map<String, Any>, @PathVariable id: String): Any {
        return HttpUtils.updated("asset", id, true, assetService.update(id, attrs))
    }

    @PutMapping(value = ["/api/v1/assets"])
    fun batchUpdate(@RequestBody req: BatchUpdateAssetsRequest): BatchUpdateAssetsResponse {
        return assetService.batchUpdate(req.assetIds, req.attrs)
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_clipChildren"])
    @Throws(IOException::class)
    fun clipChildren(@PathVariable id: String, rsp: HttpServletResponse) {
        FlipbookSender(id, searchService).serveResource(rsp)
    }

    @PostMapping(value = ["/api/v1/assets/_index"])
    @Throws(IOException::class)
    fun batchCreate(@RequestBody spec: BatchCreateAssetsRequest): BatchCreateAssetsResponse {
        return assetService.batchCreateOrReplace(spec)
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

    @PutMapping(value = ["/api/v1/refresh"])
    fun refresh() {
        logger.warn("Refresh called.")
    }
    companion object {

        private val logger = LoggerFactory.getLogger(AssetController::class.java)
    }
}
