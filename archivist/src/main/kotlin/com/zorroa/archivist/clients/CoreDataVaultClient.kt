package com.zorroa.common.clients

import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.Document
import com.zorroa.common.util.Json
import java.nio.file.Path
import java.util.*

interface CoreDataVaultClient {

    val client: RestClient

    fun getMetadata(companyId: Int, assetId: UUID): Map<String, Any>
    fun updateIndexedMetadata(companyId: Int, assetId: UUID, doc: Document) : Any
    fun getIndexedMetadata(companyId: Int, assetId: UUID) : Document
}

class IrmCoreDataVaultClientImpl constructor(url: String, serviceKey: Path) : CoreDataVaultClient {

    override val client = RestClient(url, GcpJwtSigner(serviceKey))

    override fun getMetadata(companyId: Int, assetId: UUID): Map<String, Any> {
        return client.get("/companies/$companyId/documents/assetId", Json.GENERIC_MAP)
    }

    override fun getIndexedMetadata(companyId: Int, assetId: UUID): Document {
        return client.get("/companies/$companyId/documents/$assetId/es", Document::class.java)
    }

    override fun updateIndexedMetadata(companyId: Int, assetId: UUID, doc: Document): Any {
        val response = client.put("/companies/$companyId/documents/$assetId/es", doc, Json.GENERIC_MAP)
        client.put("/companies/$companyId/documents/$assetId/fields/state/INDEXED", null, Json.GENERIC_MAP)
        return response
    }
}
