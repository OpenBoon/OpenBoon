package com.zorroa.common.service

import com.zorroa.common.clients.RestClient
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.util.Json

interface CoreDataVaultService {

    val client: RestClient

    fun updateIndexedMetadata(assetId:Asset, doc: Document) : Any
    fun getIndexedMetadata(assetId:Asset) : Document
}

class IrmCoreDataVaultServiceImpl constructor(url: String) : CoreDataVaultService {

    override val client = RestClient(url)

    override fun getIndexedMetadata(assetId: Asset): Document {
        val companyId = assetId.keys["companyId"]
        return client.get("/companies/$companyId/assets/${assetId.id}/metadata/es", Document::class.java)
    }

    override fun updateIndexedMetadata(asset:Asset, doc: Document): Any {
        val companyId : String? = doc.getAttr("irm.companyId", String::class.java)
        if (companyId == null) {
            throw IllegalStateException("Document has no companyId: ${asset.id}")
        }
        return client.put("/companies/$companyId/assets/${asset.id}/metadata/es", doc, Json.GENERIC_MAP)
    }
}
