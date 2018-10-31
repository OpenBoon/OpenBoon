package com.zorroa.common.clients

import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.security.getUserOrNull
import com.zorroa.common.util.Json
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

interface CoreDataVaultClient {

    val client: RestClient

    fun getMetadata(companyId: Int, assetId: String): Map<String, Any>
    fun updateIndexedMetadata(companyId: Int, assetId: String, doc: Document) : Any
    fun getIndexedMetadata(companyId: Int, assetId: String) : Document

    /**
     * Batch delete all assets for a given company.  Return a map of
     * assetId to boolean delete status.
     *
     * @param companyId the Int ID of the company
     * @param assetIds the array of asset ids
     * @return a Map of assetId to delete status.
     */
    fun batchDelete(companyId: Int, assetIds: List<String>): Map<String, Boolean>

    /**
     * Delete a single asset.
     *
     * @param companyId the Int company Id
     * @param assetId the asset Id
     * @return a boolean result, true if deleted.
     *
     */
    fun delete(companyId: Int, assetId: String): Boolean
}

class IrmCoreDataVaultClientImpl constructor(url: String, serviceKey: Path) : CoreDataVaultClient {

    override val client = RestClient(url, GcpJwtSigner(serviceKey))

    override fun getMetadata(companyId: Int, assetId: String): Map<String, Any> {
        return client.get("/companies/$companyId/documents/$assetId", Json.GENERIC_MAP,
                headers=getRequestHeaders())
    }

    override fun getIndexedMetadata(companyId: Int, assetId: String): Document {
        return client.get("/companies/$companyId/documents/$assetId/es", Document::class.java,
                headers=getRequestHeaders())
    }

    override fun updateIndexedMetadata(companyId: Int, assetId: String, doc: Document): Any {
        val response = client.put("/companies/$companyId/documents/$assetId/es", doc,
                Json.GENERIC_MAP, headers=getRequestHeaders())
        client.put("/companies/$companyId/documents/$assetId/fields/state/INDEXED", null, Json.GENERIC_MAP,
                headers=getRequestHeaders())
        return response
    }

    override fun batchDelete(companyId: Int, assetIds: List<String>): Map<String, Boolean> {
        return runBlocking {
            val result = ConcurrentHashMap<String, Boolean>()
            for (id in assetIds) {
                GlobalScope.launch{
                    result[id] = delete(companyId, id)
                }
            }
            result
        }
    }

    override fun delete(companyId: Int, assetId: String): Boolean {
        val result =
                client.delete("/companies/$companyId/documents/$assetId", null, Json.GENERIC_MAP,
                        headers=getRequestHeaders())
        return result["status"].toString() == "PASSED"
    }

    private inline fun getRequestHeaders() : Map<String, String>? {
        getUserOrNull()?.let {
            return mapOf(USER_HDR_KEY to it.getName())
        }
        return null
    }

    companion object {
        /**
         * IRM expects this header to be set to the value of the current username.
         */
        const val USER_HDR_KEY = "imcUserId"
    }
}
