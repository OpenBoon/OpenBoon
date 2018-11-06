package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSearchOrder
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.util.event
import com.zorroa.archivist.util.warnEvent
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.ProxySchema
import com.zorroa.common.util.Json
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths


interface IndexService {

    fun getMapping(): Map<String, Any>

    fun get(id: String): Document

    fun get(path: Path): Document

    /**
     * Return the proxy schema for the given asset.  If the asset does not have a proxy
     * schema, check to see if it has children and choose the first child.
     *
     * If there is no proxy schema anywhere, return an empty one.
     *
     * @param id
     * @return
     */
    fun getProxies(id: String): ProxySchema

    /**
     * Fetch the first page of assets.
     *
     * @return
     */
    fun getAll(page: Pager): PagedList<Document>

    fun index(assets: List<Document>): BatchCreateAssetsResponse

    fun index(doc: Document): Document

    fun removeFields(id: String, fields: MutableSet<String>)

    fun removeLink(type: String, value: String, assets: List<String>): Map<String, List<Any>>

    fun appendLink(type: String, value: String, assets: List<String>): Map<String, List<Any>>

    fun exists(path: Path): Boolean

    fun exists(id: String): Boolean

    /**
     * Update the given assetId with the supplied Map of attributes.  Return
     * the new version number of the asset.
     *
     * @param id
     * @param attrs
     * @return
     */
    fun update(assetId: String, attrs: Map<String, Any>): Long

    fun delete(assetId: String): Boolean

    fun batchDelete(assetId: List<String>): BatchDeleteAssetsResponse
}

@Component
class IndexServiceImpl  @Autowired  constructor (
        private val indexDao: IndexDao,
        private val fileServerProvider: FileServerProvider,
        private val fileStorageService: FileStorageService

) : IndexService {

    @Autowired
    lateinit var searchService: SearchService

    override fun get(id: String): Document {
        return if (id.startsWith("/")) {
            get(Paths.get(id))
        } else {
            indexDao[id]
        }
    }

    override fun get(path: Path): Document {
        return indexDao[path]
    }

    override fun getProxies(id: String): ProxySchema {
        val asset = get(id)
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)

        if (proxies != null) {
            return proxies
        } else {

            for (hit in searchService.search(Pager.first(1), AssetSearch(AssetFilter()
                    .addToTerms("media.clip.parent", id))
                    .setFields(arrayOf("proxies"))
                    .setOrder(ImmutableList.of(AssetSearchOrder("_id"))))) {
                return hit.getAttr("proxies", ProxySchema::class.java)
            }

            return ProxySchema()
        }
    }

    override fun getAll(page: Pager): PagedList<Document> {
        return indexDao.getAll(page)
    }

    override fun index(doc: Document): Document {
        val result = index(listOf(doc))
        return indexDao.get(result.assetIds[0])
    }

    override fun index(assets: List<Document>): BatchCreateAssetsResponse {

        /**
         * Clear out any protected name spaces, this lets us ensure people
         * can't manipulate them with the attr API.
         *
         * There is no guarantee the document is the full document, so we can't
         * modify the permissions/links right here since the might not exist,
         * and if they do exist we'll remove them so they don't overwrite
         * the proper value.
         */

        return indexDao.index(assets)
    }

    override fun removeFields(id: String, fields: MutableSet<String>) {
        // remove fields from list the can't remove.
        fields.removeAll(NS_PROTECTED_API)
        indexDao.removeFields(id, fields, false)
    }

    override fun removeLink(type: String, value: String, assets: List<String>): Map<String, List<Any>> {
        return indexDao.removeLink(type, value, assets)
    }

    override fun appendLink(type: String, value: String, assets: List<String>): Map<String, List<Any>> {
        return indexDao.appendLink(type, value, assets)
    }

    override fun exists(path: Path): Boolean {
        return indexDao.exists(path)
    }

    override fun exists(id: String): Boolean {
        return indexDao.exists(id)
    }

    override fun update(assetId: String, attrs: Map<String, Any>): Long {

        val asset = indexDao[assetId]
        val write = asset.getAttr("system.permissions.write", Json.SET_OF_UUIDS)

        if (!hasPermission(write)) {
            throw ArchivistWriteException("You cannot make changes to this asset.")
        }

        val copy = attrs.toMutableMap()
        /**
         * Remove keys which are maintained via other methods.
         */
        NS_PROTECTED_API.forEach { n -> copy.remove(n) }
        return indexDao.update(assetId, copy)
    }

    /**
     * Batch delete the the given asset IDs, along with their supporting files.
     * This method propagates to children as well.
     *
     * @param assetIds: the IDs of the assets the delete.
     */
    override fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse {
        /*
         * Setup an OR search where we target both the parents and children.
         */
        val search = AssetSearch()
        search.filter = AssetFilter()
        search.filter.should = listOf(
                AssetFilter().addToTerms("_id", assetIds).addToMissing("media.clip.parent"),
                AssetFilter().addToTerms("media.clip.parent", assetIds))

        /*
         * Iterate a scan and scroll and batch delete each batch.
         * Along the way queue up work to delete any files.
         */
        val rsp = BatchDeleteAssetsResponse()
        searchService.scanAndScroll(search, true) { hits->
            /*
             * Determine if any documents are on hold.
             */
            val okToDelete = mutableListOf<Document>()
            for (hit in hits.hits) {
                val doc = Document(hit.id, hit.sourceAsMap)
                if (doc.attrExists("system.hold") && doc.getAttr("system.hold", Boolean::class.java)) {
                    rsp.onHold = rsp.onHold + 1
                }
                else {
                    okToDelete.add(doc)
                }
            }

            rsp + indexDao.batchDelete(okToDelete.map { it.id })
            GlobalScope.launch {
                okToDelete.forEach {
                    deleteAssociatedFiles(it)
                }
            }
        }
        return rsp
    }

    override fun delete(assetId: String): Boolean {
        val doc = indexDao[assetId]
        deleteAssociatedFiles(doc)
        return indexDao.delete(assetId)
    }

    fun deleteAssociatedFiles(doc: Document) {
        logger.event("delete Proxy", mapOf("assetId" to doc.id))
        doc.getAttr("proxies", ProxySchema::class.java)?.let {
            it.proxies?.forEach { pr ->
                try {
                    val storage = fileStorageService.get(pr.id as String)
                    val ofile = fileServerProvider.getServableFile(storage.uri)
                    if (!ofile.delete()) {
                        logger.warnEvent("delete Proxy", "file did not exist", mapOf("proxyId" to pr.id))
                    }
                } catch (e: Exception) {
                    logger.warnEvent("delete Proxy", e.message!!, mapOf("proxyId" to pr.id), e)
                }
            }
        }
    }

    override fun getMapping(): Map<String, Any> {
        return indexDao.getMapping()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IndexServiceImpl::class.java)

        /**
         * Namespaces that are only populated via the API.  IF people manipulate these
         * wrong via the asset API then it would corrupt the asset.
         */
        private val NS_PROTECTED_API = ImmutableSet.of(
                "zorroa", "tmp")
    }

}
