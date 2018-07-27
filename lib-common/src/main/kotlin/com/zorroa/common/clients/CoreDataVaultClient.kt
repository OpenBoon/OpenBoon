package com.zorroa.common.clients

import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.util.Json

interface CoreDataVaultClient {

    val client: RestClient

    fun updateIndexedMetadata(assetId:Asset, doc: Document) : Any
    fun getIndexedMetadata(assetId:Asset) : Document
}

class IrmCoreDataVaultClientImpl constructor(url: String) : CoreDataVaultClient {

    override val client = RestClient(url)

    override fun getIndexedMetadata(assetId: Asset): Document {
        val companyId = assetId.keys["companyId"]
        return client.get("/companies/$companyId/assets/${assetId.id}/es", Document::class.java)
    }

    override fun updateIndexedMetadata(asset:Asset, doc: Document): Any {
        val companyId : String? = doc.getAttr("irm.companyId", String::class.java)
        if (companyId == null) {
            throw IllegalStateException("Document has no companyId: ${asset.id}")
        }
        return client.put("/companies/$companyId/assets/${asset.id}/es", doc, Json.GENERIC_MAP)
    }
}
