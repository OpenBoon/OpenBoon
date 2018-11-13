package com.zorroa.archivist.service

import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.clients.CoreDataVaultClient
import com.zorroa.common.domain.ArchivistSecurityException
import com.zorroa.common.domain.ArchivistWriteException
import org.springframework.beans.factory.annotation.Autowired
import java.lang.Exception

/**
 * AssetService contains the entry points for Asset CRUD operations. In general
 * you won't use IndexService directly, AssetService will call through for you
 * once the Transactional datastore is updated.
 */
interface AssetService {
    fun getDocument(assetId: String): Document
    fun setDocument(assetId: String, doc: Document)
    fun delete(assetId: String): Boolean
    fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse
}

/**
 * IrmAssetServiceImpl is a higher level wrapper around the CoreDataVaultClient that
 * requires Authentication to operate properly.
 */
class IrmAssetServiceImpl constructor(private val cdvClient: CoreDataVaultClient) : AssetService {

    @Autowired
    lateinit var indexService: IndexService

    override fun getDocument(assetId: String): Document {
        return cdvClient.getIndexedMetadata(getCompanyId(), assetId)
    }

    override fun setDocument(assetId: String, doc: Document) {
        cdvClient.updateIndexedMetadata(getCompanyId(), assetId, doc)
    }

    override fun delete(assetId: String): Boolean {
        /**
         * Relying on IRM's security to know if the asset can be deleted.
         */
        if (cdvClient.delete(getCompanyId(), assetId)) {
            return indexService.delete(assetId)
        }
        return false
    }

    override fun batchDelete(ids: List<String>): BatchDeleteAssetsResponse {
        /**
         * Relying on IRM's security to know if the assets can be deleted.
         */
        val deleted = cdvClient.batchDelete(getCompanyId(), ids)
        // Only delete from index stuff we deleted from CDV?
        return indexService.batchDelete(ids.minus(deleted.filterValues { v-> v }.keys))
    }

    /**
     * Pull the company ID from the authed user Attrs
     */
    fun getCompanyId() : Int {
        try {
            return getUser().attrs["company_id"].toString().toInt()
        }
        catch (e: Exception) {
            throw ArchivistSecurityException("Invalid company Id")
        }
    }
}

class AssetServiceImpl : AssetService {

    @Autowired
    lateinit var indexService: IndexService

    @Autowired
    lateinit var searchService: SearchService

    override fun getDocument(assetId: String): Document {
        return indexService.get(assetId)
    }

    override fun setDocument(id: String, doc: Document) {
        indexService.index(doc)
    }

    override fun delete(id: String): Boolean {
        val asset = indexService.get(id)
        if (!hasPermission("write", asset)) {
            throw ArchivistWriteException("delete access denied")
        }
        return indexService.delete(id)
    }

    override fun batchDelete(ids: List<String>): BatchDeleteAssetsResponse {
       return indexService.batchDelete(ids)
    }
}




