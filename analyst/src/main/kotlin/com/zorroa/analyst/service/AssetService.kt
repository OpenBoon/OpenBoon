package com.zorroa.analyst.service

import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.service.CoreDataVaultService
import com.zorroa.analyst.repository.IndexDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


interface AssetService {
    fun getDocument(assetId: Asset) : Document
    fun storeAndReindex(assetId: Asset, doc: Document)
}

@Service
class AssetServiceImpl @Autowired constructor(
        val coreDataVault: CoreDataVaultService,
        val indexDao: IndexDao): AssetService {

    override fun getDocument(assetId: Asset): Document {
        return coreDataVault.getIndexedMetadata(assetId)
    }

    override fun storeAndReindex(assetId: Asset, doc: Document) {
        coreDataVault.updateIndexedMetadata(assetId, doc)
        indexDao.indexDocument(assetId, doc)
    }
}
