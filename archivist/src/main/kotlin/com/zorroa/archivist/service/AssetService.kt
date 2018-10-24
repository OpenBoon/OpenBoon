package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.Document
import com.zorroa.common.clients.CoreDataVaultClient
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

interface AssetService {
    fun getDocument(asset: Asset) : Document
    fun setDocument(id: UUID, doc: Document)
}

class IrmAssetServiceImpl constructor(val cdvClient: CoreDataVaultClient) : AssetService {

    override fun getDocument(asset: Asset) : Document {
        return cdvClient.getIndexedMetadata(asset.keys["companyId"] as Int, asset.id)
    }

    override fun setDocument(id: UUID, doc: Document) {
        val asset = Asset(UUID.fromString(doc.id),
                UUID.fromString(doc.getAttr("zorroa.organizationId")),
                mutableMapOf("companyId" to doc.getAttr("irm.companyId", Int::class.java)))
        cdvClient.updateIndexedMetadata(asset.keys["companyId"] as Int, asset.id, doc)

    }
}

class AssetServiceImpl  : AssetService {

    @Autowired
    lateinit var indexService : IndexService

    override fun getDocument(asset: Asset) : Document {
        return indexService.get(asset.id.toString())
    }

    override fun setDocument(id: UUID, doc: Document) { }
}




