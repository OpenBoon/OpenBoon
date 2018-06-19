package com.zorroa.irm.studio.service

import com.zorroa.irm.studio.domain.Asset
import com.zorroa.irm.studio.domain.Document
import com.zorroa.irm.studio.repository.AssetDao
import com.zorroa.irm.studio.repository.CDVAssetSpec
import com.zorroa.irm.studio.repository.IndexDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


interface AssetService {
    fun get(orgId: UUID, id: UUID) : Asset
    fun getDocument(orgId: UUID, id: UUID) : Document
    fun storeAndReindex(orgId: UUID,  doc: Document)
}

@Service
class AssetServiceImpl @Autowired constructor(
        val assetDao: AssetDao<CDVAssetSpec>,
        val indexDao: IndexDao): AssetService {

    override fun getDocument(orgId: UUID, id: UUID): Document {
        return assetDao.getDocument(orgId, id)
    }

    override fun get(orgId: UUID, id: UUID) : Asset {
        return assetDao.get(orgId, id)
    }

    override fun storeAndReindex(orgId: UUID, doc: Document) {
        assetDao.updateDocument(orgId, UUID.fromString(doc.id), doc)
        indexDao.indexDocument(orgId, doc)
    }
}
