package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.security.canExport
import com.zorroa.common.schema.ProxySchema
import org.springframework.stereotype.Service

/**
 * Service that does Rudimentary content negotiation when returning a servable file.
 */
@Service
class AssetStreamResolutionService constructor (
    private val indexService: IndexService,
    private val fileServerProvider: FileServerProvider,
    private val fileStorageService: FileStorageService) {

    /**
     * Returns a ServableFile for an asset based on Accept header and `type` query parameter.
     */
    fun getServableFile(id: String, accept: String? = null, type: String? = null): ServableFile? {
        val asset = indexService.get(id)
        val requestedType = requestedType(accept, type)
        val forceProxy = if (requestedType == null) {
            !canExport(asset)
        } else {
            true
        }
        return if (forceProxy) {
            getServableProxy(asset, requestedType)
        } else {
            fileServerProvider.getServableFile(asset)
        }
    }

    fun requestedType(accept: String?, type: String?): String? {
        /*
           Curator asks for application/json, text/html, etc. even though it wants an image or video, so we can't just
           resolve the type directly.  We filter down to either image or video.
           If neither video or image was requested we just return the type query parameter or null if there wasn't one.
         */
        val allowedTypes = setOf("image", "video")
        val types = accept?.split(Regex(",\\p{Blank}?")) ?: emptyList()
        val resolvedType = types.firstOrNull { allowedTypes.contains(it.substringBefore("/")) } ?: type
        return resolvedType?.substringBefore("/")
    }

    private fun getServableProxy(asset: Document, type: String?): ServableFile? {
        /*
            This preserves the original behavior, but is probably not ideal.
            Users not having export permission will get an image proxy if allowed to `download`
            from lightbox, even if the asset is not an `image`.
         */
        val proxy = getProxyStream(asset, type ?: "image")
        if (proxy != null) {
            return fileServerProvider.getServableFile(proxy.uri)
        }
        return null
    }

    private fun getProxyStream(asset: Document, type: String): FileStorage? {
        // If the file doesn't have a proxy this will throw.
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)
        if (proxies != null) {
            val largest = proxies.getLargest(type)
            if (largest != null) {
                return fileStorageService.get(largest.id)
            }
        }
        return null
    }

}
