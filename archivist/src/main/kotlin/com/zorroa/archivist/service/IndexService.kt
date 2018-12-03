package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSearchOrder
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.service.AbstractAssetService.Companion.PROTECTED_NAMESPACES
import com.zorroa.archivist.util.event
import com.zorroa.archivist.util.warnEvent
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.ProxySchema
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths

/**
 * The IndexService is responsible for the business logic around asset CRUD and batch operations.
 */
interface IndexService {

    fun getMapping(): Map<String, Any>

    fun get(id: String): Document

    fun get(path: Path): Document

    fun getProxies(id: String): ProxySchema

    fun getAll(page: Pager): PagedList<Document>

    fun index(assets: List<Document>): BatchCreateAssetsResponse

    fun index(doc: Document): Document

    fun removeLink(type: String, value: String, assets: List<String>): Map<String, List<Any>>

    fun appendLink(type: String, value: String, assets: List<String>): Map<String, List<Any>>

    fun exists(path: Path): Boolean

    fun exists(id: String): Boolean

    fun update(doc: Document, attrs: Map<String, Any>): Document

    fun delete(assetId: String): Boolean

    fun batchDelete(assetId: List<String>): BatchDeleteAssetsResponse
}

@Component
class IndexServiceImpl  @Autowired  constructor (
        private val indexDao: IndexDao,
        private val auditLogDao: AuditLogDao,
        private val fileServerProvider: FileServerProvider,
        private val fileStorageService: FileStorageService

) : IndexService {

    @Autowired
    lateinit var searchService: SearchService

    override fun get(id: String): Document {
        return if (id.startsWith("/")) {
            get(Paths.get(id))
        } else {
            indexDao.get(id)
        }
    }

    override fun get(path: Path): Document {
        return indexDao.get(path)
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
        index(listOf(doc))
        return indexDao.get(doc.id)
    }

    override fun index(assets: List<Document>): BatchCreateAssetsResponse {
        return indexDao.index(assets)
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

    override fun update(doc: Document, attrs: Map<String, Any>): Document {
        /**
         * Make a copy and remove fields which are maintained via other methods.
         */
        PROTECTED_NAMESPACES.forEach { doc.removeAttr(it) }
        val auditLogs = mutableListOf<AuditLogEntrySpec>()
        attrs.forEach {
            val ns = it.key.split('.')
            try {
                if (ns[0] !in PROTECTED_NAMESPACES) {
                    doc.setAttr(it.key, it.value)
                    auditLogs.add(AuditLogEntrySpec(doc.id, AuditLogType.Changed, field = it.key, value = it.value))
                } else {
                    logger.warnEvent("update Asset",
                            "Attempted to set protected namespace ${it.key}", emptyMap())
                }
            } catch (e: Exception) {
                logger.warnEvent("update Asset",
                        "Attempted to set invalid namespace ${it.key}", emptyMap())
            }
        }
        indexDao.update(doc)
        auditLogDao.batchCreate(auditLogs)
        return indexDao.get(doc.id)
    }

    /**
     * Batch delete the the given asset IDs, along with their supporting files.
     * This method propagates to children as well.
     *
     * @param assetIds: the IDs of the assets the delete.
     */
    override fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse {
        if (assetIds.size > 1000) {
            throw ArchivistWriteException("Unable to delete more than 1000 assets in a single request")
        }

        if (assetIds.isEmpty()) {
            return BatchDeleteAssetsResponse()
        }

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
            val docs = hits.hits.map { Document(it.id, it.sourceAsMap) }
            val batchRsp = indexDao.batchDelete(docs)

            auditLogDao.batchCreate(batchRsp.deletedAssetIds.map {
                AuditLogEntrySpec(it, AuditLogType.Deleted)
            })

            // add the batch results to the overall result.
            rsp.plus(batchRsp)

            GlobalScope.launch {
                docs.forEach {
                    if (it.id in batchRsp.deletedAssetIds) {
                        deleteAssociatedFiles(it)
                    }
                }
            }
        }
        return rsp
    }

    override fun delete(assetId: String): Boolean {
        val doc = indexDao.get(assetId)
        if (!hasPermission("write", doc)) {
            throw ArchivistWriteException("delete access denied")
        }

        val result = indexDao.delete(assetId)
        deleteAssociatedFiles(doc)
        if (result) {
            auditLogDao.create(AuditLogEntrySpec(assetId, AuditLogType.Deleted))
        }
        return result
    }

    fun deleteAssociatedFiles(doc: Document) {
        logger.event("deleteAll assetProxy", mapOf("assetId" to doc.id))
        doc.getAttr("proxies", ProxySchema::class.java)?.let {
            it.proxies?.forEach { pr ->
                try {
                    val storage = fileStorageService.get(pr.id)
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
    }

}
