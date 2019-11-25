package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.schema.ProxySchema
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

/**
 * Service that does Rudimentary content negotiation when returning a [ServableFile].
 */
@Service
class AssetStreamResolutionService constructor(
    private val assetService: AssetService,
    private val fileServerProvider: FileServerProvider,
    private val fileStorageService: FileStorageService
) {

    /**
     * Return a [ServableFile] for the given asset Id and list of [MediaType]s
     *
     * @param id The unique ID of the asset.
     * @param types The acceptable [MediaType]s to serve.
     * @return A [ServableFile] or null none could be found.
     */
    fun getServableFile(id: String, types: List<MediaType>): ServableFile? {
        val asset = assetService.get(id)
        val canDisplay = canDisplaySource(asset, types)
        // TODO: ZMLP always force proxy?
        val forceProxy = true
        val sourceFile = fileServerProvider.getServableFile(asset)

        /**
         * If the we have a video clip AND video proxies exist, then use video
         * proxies because it's the only way to guarantee the movie is
         * a fast-start movie.
         */
        val proxies = asset.getAttr("proxies", ProxySchema::class.java) ?: ProxySchema()
        val forceVideoProxy = asset.attrExists("media.clip.parent") &&
            asset.getAttr<String>("source.type") == "video" &&
            proxies.getLargest("video") != null

        /**
         * Three things have to checkout or else the proxy is served.
         * 1. The proxy was forced by lack of permissions.
         * 2. The source file is not displayable by the application making request (set by accept header)
         * 3. The source file does not exist. (common M/E case)
         * 4. If we have a video clip with video proxies, use the proxies.
         */
        if (logger.isDebugEnabled) {
            logger.debug("Select playback media : hasAccess: {} clientCanDisplay: {} exists: {} forceVideoProxy: {}",
                forceProxy, canDisplay, sourceFile.exists(), forceVideoProxy)
        }

        return if (forceProxy || !canDisplay || !sourceFile.exists() || forceVideoProxy) {
            getProxy(asset, types)
        } else {
            sourceFile
        }
    }

    /**
     * Return a [ServableFile] that points to a valid proxy of the given types.
     *
     * If the mimeTypes list is empty, then search for proxies with the same overall
     * type as the source file, then fall back to an image proxy last.
     *
     * @param asset The document with the proxies.
     * @param mimeTypes A list of [MimeType]s.
     * @return A [ServableFile] or null if one can't be found.
     *
     */
    fun getProxy(asset: Asset, mimeTypes: List<MediaType>): ServableFile? {

        /**
         * Grab the proxies or return null.
         */
        val proxies = asset.getAttr("proxies", ProxySchema::class.java) ?: return null

        if (mimeTypes.isEmpty()) {
            for (type in linkedSetOf(asset.getAttr<String>("source.type") ?: "image", "image")) {
                val proxy = proxies.getLargest(type)
                if (proxy != null) {
                    return fileStorageService.get(proxy.id).getServableFile()
                }
            }
        } else {
            for (type in mimeTypes) {
                val proxy = proxies.getLargest(type)
                if (proxy != null) {
                    return fileStorageService.get(proxy.id).getServableFile()
                }
            }
        }

        return null
    }

    /**
     * Return true if the asset's mimeType is in the list of types.  If the mimeTypes
     * list is empty or contains *, then return true.
     *
     * @param asset The asset to check.
     * @param mimeTypes The accepted list of mimeTypes
     * @return True if the asset's mimeType is in the list of types
     */
    fun canDisplaySource(asset: Asset, mimeTypes: List<MediaType>): Boolean {
        /**
         * If no acceptable types are sent, or all types are allowed, then return true.
         */
        if (mimeTypes.isEmpty() || MediaType.ALL in mimeTypes) {
            return true
        }

        val sourceMediaType = MediaType.parseMediaType(asset.getAttr("source.mediaType"))
        return sourceMediaType in mimeTypes
    }

    companion object {

        private val logger = LoggerFactory.getLogger(AssetStreamResolutionService::class.java)
    }
}
