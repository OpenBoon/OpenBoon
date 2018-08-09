package com.zorroa.archivist.service

import com.zorroa.common.clients.CoreDataVaultClient
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

interface AssetService {
    fun getDocument(asset: Asset) : Document
    fun setDocument(id: UUID, doc: Document)
}

class IrmAssetServiceImpl constructor(val cdvClient: CoreDataVaultClient) : AssetService {

    override fun getDocument(asset: Asset) : Document {
        return cdvClient.getIndexedMetadata(asset)
    }

    override fun setDocument(id: UUID, doc: Document) {

        val asset = Asset(UUID.fromString(doc.id),
                UUID.fromString(doc.getAttr("zorroa.organizationId")),
                mutableMapOf("companyId" to doc.getAttr("irm.companyId", Int::class.java)))
        cdvClient.updateIndexedMetadata(asset, doc)
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




