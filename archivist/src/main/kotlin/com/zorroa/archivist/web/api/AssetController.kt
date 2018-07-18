package com.zorroa.archivist.web.api

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.HttpUtils.CACHE_CONTROL
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.HideField
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.UserLogSpec
import com.zorroa.archivist.repository.AssetIndexResult
import com.zorroa.archivist.security.canExport
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.service.*
import com.zorroa.archivist.web.MultipartFileSender
import com.zorroa.archivist.web.sender.FlipbookSender
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.domain.Document
import com.zorroa.common.domain.PagedList
import com.zorroa.common.domain.Pager
import com.zorroa.common.filesystem.ObjectFileSystem
import com.zorroa.common.schema.Proxy
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.search.AssetSearch
import com.zorroa.common.search.AssetSuggestBuilder
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class AssetController @Autowired constructor(
        private val indexService: IndexService,
        private val searchService: SearchService,
        private val folderService: FolderService,
        private val logService: EventLogService,
        private val imageService: ImageService,
        private val ofs: ObjectFileSystem,
        private val fieldService: FieldService
){
    /**
     * Describes a file to stream.
     */
    class StreamFile(var path: String, var mimeType: String, var proxy: Boolean)

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

    fun getPreferredFormat(asset: Document, preferExt: String?, fallback: Boolean, streamProxy: Boolean): StreamFile? {

        val mediaType = asset.getAttr("source.mediaType", String::class.java)

        /**
         * Zorroa types get handled special.
         */
        if (streamProxy) {
            return getProxyStream(asset)
        } else {
            val checkFiles = Lists.newArrayList<StreamFile>()
            val path = asset.getAttr("source.path", String::class.java)
            val type = asset.getAttr("source.type", String::class.java)

            if (preferExt != null) {
                /**
                 * If preferExt is set, then first we check the assets source directory for
                 * a file with that ext.
                 */
                val preferPath = path.substring(0, path.lastIndexOf('.') + 1) + preferExt
                val preferMediaType = tika.detect(path)
                checkFiles.add(StreamFile(preferPath, preferMediaType, false))

                val proxies = asset.getAttr("proxies.$type", object : TypeReference<List<Proxy>>() {})
                if (proxies != null) {
                    for (proxy in proxies) {
                        if (preferExt == proxy.format) {
                            val f = ofs.get(proxy.id)
                            if (f.exists()) {
                                checkFiles.add(StreamFile(f.file.toString(),
                                        tika.detect(f.file.toString()), false))
                                break
                            }
                        }
                    }
                }
            } else {
                checkFiles.add(StreamFile(path, mediaType, false))
            }

            for (sf in checkFiles) {
                if (File(sf.path).exists()) {
                    return sf
                }
            }

            return if (fallback) {
                getProxyStream(asset)
            } else {
                null
            }
        }
    }

    fun getProxyStream(asset: Document): StreamFile? {
        // If the file doesn't have a proxy this will throw.
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)
        if (proxies != null) {
            val largest = proxies.getLargest()
            if (largest != null) {
                return StreamFile(
                        ofs.get(largest.id).file.toString(),
                        (PROXY_MIME_LOOKUP as java.util.Map<String, String>).getOrDefault(largest.format,
                                "application/octet-stream"), true)
            }
        }

        return null
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_stream"])
    @Throws(Exception::class)
    fun streamAsset(@RequestParam(defaultValue = "true", required = false) fallback: Boolean,
                    @RequestParam(value = "ext", required = false) ext: String?,
                    @PathVariable id: String, request: HttpServletRequest, response: HttpServletResponse) {

        val asset = indexService.get(id)
        val canExport = canExport(asset)
        val format = getPreferredFormat(asset, ext, fallback, !canExport)

        if (format == null) {
            response.status = 404
        } else {
            try {
                MultipartFileSender.fromPath(Paths.get(format.path))
                        .with(request)
                        .with(response)
                        .setContentType(format.mimeType)
                        .serveResource()
                if (canExport) {
                    logService.logAsync(UserLogSpec.build(LogAction.View, "asset", asset.id))
                }
            } catch (e: Exception) {
                logger.warn("MultipartFileSender failed on {}, unexpected {}", id, e.message)
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
                        e[0] == "closest" -> proxies.getClosest(e[2].toInt(), e[3].toInt())
                        e[0] == "atLeast" -> proxies.atLeastThisSize(e[2].toInt())
                        e[0] == "smallest" -> proxies.getSmallest()
                        e[0] == "largest" -> proxies.getLargest()
                        else -> proxies.getLargest()
                    }
                }
            })

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/closest/{width:\\d+}x{height:\\d+}"])
    @Throws(IOException::class)
    fun getClosestProxy(req: HttpServletRequest,
                        rsp: HttpServletResponse,
                        @PathVariable id: String,
                        @PathVariable width: Int,
                        @PathVariable height: Int): ResponseEntity<InputStreamResource> {
        return try {
            rsp.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            imageService.serveImage(req, proxyLookupCache.get("closest:$id:$width:$height"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/atLeast/{size:\\d+}"])
    @Throws(IOException::class)
    fun getAtLeast(req: HttpServletRequest,
                   rsp: HttpServletResponse,
                   @PathVariable id: String,
                   @PathVariable(required = true) size: Int): ResponseEntity<InputStreamResource> {
        return try {
            rsp.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            imageService.serveImage(req, proxyLookupCache.get("atLeast:$id:$size"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/largest"])
    @Throws(IOException::class)
    fun getLargestProxy(req: HttpServletRequest,
                        rsp: HttpServletResponse,
                        @PathVariable id: String): ResponseEntity<InputStreamResource> {
        return try {
            rsp.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            imageService.serveImage(req, proxyLookupCache.get("largest:$id"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/smallest"])
    @Throws(IOException::class)
    fun getSmallestProxy(req: HttpServletRequest,
                         rsp: HttpServletResponse,
                         @PathVariable id: String): ResponseEntity<InputStreamResource> {
        return try {
            rsp.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            imageService.serveImage(req, proxyLookupCache.get("smallest:$id"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
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
    fun getV2(@PathVariable id: String): Any {
        return indexService.get(id)
    }

    @GetMapping(value = ["/api/v1/assets/_path"])
    fun getByPath(@RequestBody path: Map<String, String>): Document? {
        return path["path"]?.let { indexService.get(Paths.get(it)) }
    }

    @DeleteMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    fun delete(@PathVariable id: String): Any {
        val asset = indexService.get(id)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("delete access denied")
        }

        val result = indexService.delete(id)
        return HttpUtils.deleted("asset", id, result)
    }

    @PutMapping(value = ["/api/v1/assets/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(IOException::class)
    fun update(@RequestBody attrs: Map<String, Any>, @PathVariable id: String): Any {
        val asset = indexService.get(id)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }

        indexService.update(id, attrs)
        return HttpUtils.updated("asset", id, true, indexService.get(id))
    }

    @DeleteMapping(value = ["/api/v1/assets/{id}/_fields"])
    @Throws(IOException::class)
    fun removeFields(@RequestBody fields: MutableSet<String>, @PathVariable id: String): Any {
        indexService.removeFields(id, fields)
        return HttpUtils.updated("asset", id, true, indexService.get(id))
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_clipChildren"])
    @Throws(IOException::class)
    fun clipChildren(@PathVariable id: String, rsp: HttpServletResponse) {
        FlipbookSender(id, searchService).serveResource(rsp)
    }

    @PostMapping(value = ["/api/v1/assets/_index"], produces = [(MediaType.APPLICATION_JSON_VALUE)])
    @Throws(IOException::class)
    fun index(@RequestBody spec: AssetIndexSpec): AssetIndexResult {
        return indexService.index(spec)
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

    class SetPermissionsRequest {
        var search: AssetSearch? = null
        var acl: Acl? = null

    }

    /*
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SHARE) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/assets/_permissions"])
    @Throws(Exception::class)
    fun setPermissions(
            @Valid @RequestBody req: SetPermissionsRequest): Command {
        val spec = CommandSpec()
        spec.type = CommandType.UpdateAssetPermissions
        spec.args = arrayOf(req.search, req.acl)
        return commandService.submit(spec)
    }
    */

    @PutMapping(value = ["/api/v1/refresh"])
    fun refresh() {
        logger.warn("Refresh called.")
    }
    companion object {

        private val logger = LoggerFactory.getLogger(AssetController::class.java)

        private val tika = Tika()

        /**
         * We could try to detect it using something like Tika but
         * there are only a couple types.
         */
        private val PROXY_MIME_LOOKUP = ImmutableMap.of("png", "image/png",
                "jpg", "image/jpeg")
    }
}
