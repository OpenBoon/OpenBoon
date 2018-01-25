package com.zorroa.archivist.web.api

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.HttpUtils.CACHE_CONTROL
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.archivist.service.*
import com.zorroa.archivist.web.MultipartFileSender
import com.zorroa.common.elastic.ElasticClientUtils
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.*
import com.zorroa.sdk.filesystem.ObjectFileSystem
import com.zorroa.sdk.schema.ProxySchema
import com.zorroa.sdk.search.AssetSearch
import com.zorroa.sdk.search.AssetSuggestBuilder
import org.apache.tika.Tika
import org.elasticsearch.ResourceNotFoundException
import org.elasticsearch.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
import java.util.concurrent.TimeUnit
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@RestController
class AssetController @Autowired constructor(
        private val client: Client,
        private val assetService: AssetService,
        private val noteService: NoteService,
        private val searchService: SearchService,
        private val logService: EventLogService,
        private val imageService: ImageService,
        private val ofs: ObjectFileSystem,
        private val commandService: CommandService
){

    /**
     * Describes a file to stream.
     */
    class StreamFile(var path: String, var mimeType: String, var proxy: Boolean)

    @Value("\${zorroa.cluster.index.alias}")
    private lateinit var alias: String

    @GetMapping(value = ["/api/v1/assets/_fields"])
    fun getFields(response: HttpServletResponse) : Map<String, Set<String>> {
        response.setHeader("Cache-Control", CacheControl.maxAge(
                30, TimeUnit.SECONDS).cachePrivate().headerValue)
        return searchService.getFields("asset")
    }

    val elementFields: Map<String, Set<String>>
        @GetMapping(value = ["/api/v1/elements/_fields"])
        @Throws(IOException::class)
        get() = searchService.getFields("element")

    val mapping: Map<String, Any>
        @GetMapping(value = ["/api/v1/assets/_mapping"])
        @Throws(IOException::class)
        get() = assetService.getMapping()

    fun getPreferredFormat(asset: Document, preferExt: String?, fallback: Boolean, streamProxy: Boolean): StreamFile? {
        if (streamProxy) {
            return getProxyStream(asset)
        } else {

            val checkFiles = Lists.newArrayList<StreamFile>()

            val path = asset.getAttr("source.path", String::class.java)
            val mediaType = asset.getAttr("source.mediaType", String::class.java)
            val type = asset.getAttr("source.type", String::class.java)

            if (preferExt != null) {
                /**
                 * If preferExt is set, then first we check the assets source directory for
                 * a file with that ext.
                 */
                val preferPath = path.substring(0, path.lastIndexOf('.') + 1) + preferExt
                val preferMediaType = tika.detect(path)
                checkFiles.add(StreamFile(preferPath, preferMediaType, false))

                val proxies = asset.getAttr("proxies." + type, object : TypeReference<List<Proxy>>() {

                })
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
            val largest = proxies.largest
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

        val asset = assetService.get(id)
        val canExport = SecurityUtils.canExport(asset)
        val format = getPreferredFormat(asset, ext, fallback, !canExport)

        /*
         * Nothing to return...
         */
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

    @GetMapping(value = ["/api/v1/assets/{id}/notes"])
    @Throws(IOException::class)
    fun getNotes(@PathVariable id: String): List<Note> {
        return noteService.getAll(id)
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/closest/{size:\\d+x\\d+}"])
    @Throws(IOException::class)
    fun getClosestProxy(response: HttpServletResponse, @PathVariable id: String, @PathVariable(required = false) size: String): ResponseEntity<InputStreamResource> {
        try {
            val wh = size.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val proxies = assetService.getProxies(id)
            val proxy = proxies.getClosest(Integer.valueOf(wh[0]), Integer.valueOf(wh[1])) ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
            response.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            return imageService.serveImage(proxy)
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/at-least/{size:\\d+}"])
    @Throws(IOException::class)
    fun getAtLeast(response: HttpServletResponse, @PathVariable id: String, @PathVariable(required = true) size: Int): ResponseEntity<InputStreamResource> {
        try {
            val proxies = assetService.getProxies(id)
            val proxy = proxies.atLeastThisSize(size) ?: proxies.largest
            response.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            return imageService.serveImage(proxy)
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/largest"])
    @Throws(IOException::class)
    fun getLargestProxy(response: HttpServletResponse, @PathVariable id: String): ResponseEntity<InputStreamResource> {
        try {
            val proxies = assetService.getProxies(id)
            val proxy = proxies.largest ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
            response.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            return imageService.serveImage(proxy)
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }

    }

    @GetMapping(value = ["/api/v1/assets/{id}/proxies/smallest"])
    @Throws(IOException::class)
    fun getSmallestProxy(response: HttpServletResponse, @PathVariable id: String): ResponseEntity<InputStreamResource> {
        try {
            val proxies = assetService.getProxies(id)
            val proxy = proxies.smallest ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null)
            response.setHeader("Cache-Control", CACHE_CONTROL.headerValue)
            return imageService.serveImage(proxy)
        } catch (e: Exception) {
            throw ResourceNotFoundException(e.message)
        }

    }

    @PostMapping(value = ["/api/v2/assets/_search"])
    @Throws(IOException::class)
    fun searchV2(@RequestBody search: AssetSearch, httpResponse: HttpServletResponse) {
        httpResponse.contentType = MediaType.APPLICATION_JSON_VALUE
        val response = searchService.search(search)
        HttpUtils.writeElasticResponse(response, httpResponse)
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
                searchService.updateField(update.setHide(true).setManual(true)))
    }

    @DeleteMapping(value = ["/api/v1/assets/_fields/hide"])
    @Throws(IOException::class)
    fun hideField(@RequestBody update: HideField): Any {
        return HttpUtils.status("field", "unhide",
                searchService.updateField(update.setHide(false)))
    }

    @PostMapping(value = ["/api/v2/assets/_count"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(IOException::class)
    fun count(@RequestBody search: AssetSearch): Any {
        return HttpUtils.count(searchService.count(search))
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_exists"])
    @Throws(IOException::class)
    fun exists(@PathVariable id: String): Any {
        return HttpUtils.exists(id, assetService.exists(id))
    }

    @PostMapping(value = ["/api/v2/assets/_suggest"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(IOException::class)
    fun suggestV2(@RequestBody builder: AssetSuggestBuilder): String {
        val response = searchService.suggest(builder.text)
        return response.toString()
    }

    @PostMapping(value = ["/api/v3/assets/_suggest"])
    @Throws(IOException::class)
    fun suggestV3(@RequestBody suggest: AssetSuggestBuilder): Any {
        return searchService.getSuggestTerms(suggest.text)
    }

    @GetMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    operator fun get(@PathVariable id: String, httpResponse: HttpServletResponse) {
        val response = client.prepareGet(alias, "asset", id).get()
        HttpUtils.writeElasticResponse(response, httpResponse)
    }

    @GetMapping(value = ["/api/v2/assets/{id}"])
    fun getV2(@PathVariable id: String): Any {
        return assetService.get(id)
    }

    @GetMapping(value = ["/api/v1/assets/_path"])
    fun getByPath(@RequestBody path: Map<String, String>): Document? {
        return path["path"]?.let { assetService.get(Paths.get(it)) }
    }

    @GetMapping(value = ["/api/v1/assets/{id}/_elements"])
    fun getElements(@PathVariable id: String,
                    @RequestParam(value = "from", required = false) from: Int?,
                    @RequestParam(value = "count", required = false) count: Int?): PagedList<Document> {
        return assetService.getElements(id, Pager(from, count))
    }

    @DeleteMapping(value = ["/api/v1/assets/{id}"])
    @Throws(IOException::class)
    fun delete(@PathVariable id: String): Any {
        val asset = assetService.get(id)
        if (!SecurityUtils.hasPermission("write", asset)) {
            throw ArchivistWriteException("delete access denied")
        }

        val result = assetService.delete(id)
        return HttpUtils.deleted("asset", id, result)
    }

    @PutMapping(value = ["/api/v1/assets/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Throws(IOException::class)
    fun update(@RequestBody attrs: Map<String, Any>, @PathVariable id: String): Any {
        val asset = assetService.get(id)
        if (!SecurityUtils.hasPermission("write", asset)) {
            throw ArchivistWriteException("update access denied")
        }

        assetService.update(id, attrs)
        return HttpUtils.updated("asset", id, true, assetService.get(id))
    }

    @DeleteMapping(value = ["/api/v1/assets/{id}/_fields"])
    @Throws(IOException::class)
    fun removeFields(@RequestBody fields: MutableSet<String>, @PathVariable id: String): Any {
        assetService.removeFields(id, fields)
        return HttpUtils.updated("asset", id, true, assetService.get(id))
    }

    @RequestMapping(value = ["/api/v1/assets/_index"], method = arrayOf(RequestMethod.POST), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @Throws(IOException::class)
    fun index(@RequestBody spec: AssetIndexSpec): AssetIndexResult {
        return assetService.index(spec)
    }

    class SetPermissionsRequest {
        var search: AssetSearch? = null
        var acl: Acl? = null

    }

    @PreAuthorize("hasAuthority('group::share') || hasAuthority('group::administrator')")
    @PutMapping(value = ["/api/v1/assets/_permissions"])
    @Throws(Exception::class)
    fun setPermissions(
            @Valid @RequestBody req: SetPermissionsRequest): Command {
        val spec = CommandSpec()
        spec.type = CommandType.UpdateAssetPermissions
        spec.args = arrayOf(req.search, req.acl)
        return commandService.submit(spec)
    }

    @PutMapping(value = ["/api/v1/refresh"])
    fun refresh() {
        ElasticClientUtils.refreshIndex(client, 0)
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
