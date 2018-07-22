package com.zorroa.analyst.service

import com.zorroa.analyst.domain.UpdateStatus
import com.zorroa.analyst.repository.IndexDao
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.service.CoreDataVaultService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


interface AssetService {
    fun getDocument(assetId: Asset) : Document
    fun storeAndReindex(assetId: Asset, doc: Document) : UpdateStatus
}

@Service
class AssetServiceImpl @Autowired constructor(
        val coreDataVault: CoreDataVaultService,
        val indexDao: IndexDao): AssetService {

    override fun getDocument(assetId: Asset): Document {
        return coreDataVault.getIndexedMetadata(assetId)
    }

    override fun storeAndReindex(assetId: Asset, doc: Document) : UpdateStatus {
        val result = UpdateStatus()
        try {
            coreDataVault.updateIndexedMetadata(assetId, doc)
            result.status["stored"] = true
            logger.info("Stored: {}", assetId.id)
        } catch (e: Exception) {
            logger.warn("Failed to write {} into CDV", assetId.id,e)
        }
        try {
            indexDao.indexDocument(assetId, doc)
            result.status["indexed"] = true
            logger.info("Indexed: {}", assetId.id)
        } catch(e: Exception) {
            logger.warn("Failed to write {} into Index", assetId.id,e)
        }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    }
}
