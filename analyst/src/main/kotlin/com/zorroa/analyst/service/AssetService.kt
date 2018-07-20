package com.zorroa.analyst.service

import com.google.common.collect.ImmutableSet
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.service.CoreDataVaultService
import com.zorroa.analyst.repository.IndexDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


interface AssetService {
    fun getDocument(assetId: Asset) : Document
    fun storeAndReindex(assetId: Asset, doc: Document)
    fun removeIllegalNamespaces(doc: Document)
}

@Service
class AssetServiceImpl @Autowired constructor(
        val coreDataVault: CoreDataVaultService,
        val indexDao: IndexDao): AssetService {

    companion object {
        /**
         * Namespaces that are only populated via the API.  IF people manipulate these
         * incorrectly via the asset API then it would corrupt the asset.
         */
        private val NS_PROTECTED_API = ImmutableSet.of(
                "zorroa", "tmp")
    }

    override fun getDocument(assetId: Asset): Document {
        return coreDataVault.getIndexedMetadata(assetId)
    }

    override fun storeAndReindex(assetId: Asset, doc: Document) {
        removeIllegalNamespaces(doc)
        coreDataVault.updateIndexedMetadata(assetId, doc)
        indexDao.indexDocument(assetId, doc)
    }

    override fun removeIllegalNamespaces(doc: Document) {
        /**
         * Removes illegal namespaces from the [Document].
         **/
        NS_PROTECTED_API.forEach { n -> doc.removeAttr(n) }
    }




}
