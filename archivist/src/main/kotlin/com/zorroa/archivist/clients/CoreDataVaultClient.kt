package com.zorroa.common.clients

import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.security.getUserOrNull
import com.zorroa.archivist.util.warnEvent
import com.zorroa.common.util.Json
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * The properties required to make a brand new core data vault asset.
 *
 * @property documentGUID: The UUID of the asset.
 * @property fileName: The file name
 * @property documentTypeId The doc type ID.
 */
class CoreDataVaultAssetSpec (
    val documentGUID: String,
    val documentTypeId: String,
    val fileName: String

) {
    constructor(doc: Document, documentTypeId: String) :
            this(doc.id, documentTypeId, doc.getAttr("source.path", String::class.java))

    constructor(documentGUID: UUID, documentTypeId: String, fileName: String) :
            this(documentGUID.toString(), documentTypeId, fileName)
}

interface CoreDataVaultClient {

    val client: RestClient

    /**
     * Get the assets core metadata.
     *
     * @param companyId the company id
     * @param assetId the asset Id
     * @return Map<String, Any>
     */
    fun getAsset(companyId: Int, assetId: String): Map<String, Any>

    /**
     * Return the hard copy of indexed metadata from CDV.
     *
     * @param companyId the company ID
     * @param assetId: the asset ID, same as the zorroa doc id.
     * @return Document
     */
    fun getIndexedMetadata(companyId: Int, assetId: String) : Document

    /**
     * Update an asset's plain old metadata and return the new asset.
     * (not its indexed metadata)
     *
     * @param companyId Int
     * @return Map<String, Any>
     */
    fun updateAsset(companyId: Int, spec: Map<String, Any>) : Map<String, Any>

    /**
     * Create a brand new asset.
     *
     * @param companyId the company ID.
     * @param spec the properties required to make a new asset.
     */
    fun createAsset(companyId: Int, spec: CoreDataVaultAssetSpec): Map<String, Any>

    /**
     * Get a list of all document types by company Id.
     *
     * @param companyId the Int ID of the company
     * @return List<Map<String, Any>>
     */
    fun getDocumentTypes(companyId: Int) : List<Map<String, Any>>

    /**
     * Update the indexed metadata for a given asset.  Return True if the metadata was updated,
     * false if not.  The id embedded in the Document object is used to call CDV server.
     *
     * @param companyId The id of the company
     * @param doc The document to use.
     * @return Boolean if the asset was updated
     */
    fun updateIndexedMetadata(companyId: Int, doc: Document) : Boolean

    /**
     * Batch update the indexed metadata for all assets for a given company.  Return a map of
     * assetId to boolean to updated status
     *
     * @param companyId the Int ID of the company
     * @param assetIds the array of asset ids
     * @return a Map of assetId to delete status.
     */
    fun batchUpdateIndexedMetadata(companyId: Int, docs: List<Document>) : Map<String, Boolean>

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
     */
    fun delete(companyId: Int, assetId: String): Boolean
}

class IrmCoreDataVaultClientImpl constructor(url: String, serviceKey: Path) : CoreDataVaultClient {

    override val client = RestClient(url, GcpJwtSigner(serviceKey))

    init {
        logger.info("Initialized CDV REST client $url")
    }

    override fun createAsset(companyId: Int, spec: CoreDataVaultAssetSpec) : Map<String, Any> {
        return client.post("/companies/$companyId/documents", spec, Json.GENERIC_MAP,
                headers=getRequestHeaders())
    }

    override fun updateAsset(companyId: Int, asset: Map<String, Any>) : Map<String, Any> {
        val id = asset["documentGUID"]
        return client.put("/companies/$companyId/documents/$id", asset, Json.GENERIC_MAP,
                headers=getRequestHeaders())
    }

    override fun getAsset(companyId: Int, assetId: String): Map<String, Any> {
        return client.get("/companies/$companyId/documents/$assetId", Json.GENERIC_MAP,
                headers=getRequestHeaders())
    }

    override fun getIndexedMetadata(companyId: Int, assetId: String): Document {
        return client.get("/companies/$companyId/documents/$assetId/es", Document::class.java,
                headers=getRequestHeaders())
    }

    override fun updateIndexedMetadata(companyId: Int, doc: Document) : Boolean {
        val assetId = doc.id
        return try {
            val response = client.put("/companies/$companyId/documents/$assetId/es", doc,
                    Json.GENERIC_MAP, headers = getRequestHeaders())
            client.put("/companies/$companyId/documents/$assetId/fields/state/INDEXED", null, Json.GENERIC_MAP,
                    headers = getRequestHeaders())
            response["status"] == "PASSED"
        } catch (e: Exception) {
            logger.warnEvent("updateIndexMetadata Asset", e.message ?: "No error message",
                    mapOf("companyId" to companyId, "assetId" to assetId))
            false
        }
    }

    override fun batchUpdateIndexedMetadata(companyId: Int, docs: List<Document>) : Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        val deferred = docs.map {
            GlobalScope.async {
                Pair(it.id, updateIndexedMetadata(companyId, it))
            }
        }

        runBlocking {
            deferred.map {
                val r = it.await()
                result.put(r.first, r.second)
            }
        }
        return result
    }

    override fun batchDelete(companyId: Int, assetIds: List<String>): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        val deferred = assetIds.map {
            GlobalScope.async {
                Pair(it, delete(companyId, it))
            }
        }
        runBlocking {
            deferred.map {
                val r = it.await()
                result.put(r.first, r.second)
            }
        }
        return result
    }

    override fun delete(companyId: Int, assetId: String): Boolean {
        val result =
                client.delete("/companies/$companyId/documents/$assetId", null, Json.GENERIC_MAP,
                        headers=getRequestHeaders())
        return result["status"].toString() == "PASSED"
    }

    override fun getDocumentTypes(companyId: Int): List<Map<String, Any>> {
        val result =
                client.get("/companies/$companyId/documentTypes", Json.GENERIC_MAP, headers=getRequestHeaders())
        return result["data"] as List<Map<String, Any>>
    }


    private inline fun getRequestHeaders() : Map<String, String>? {
        getUserOrNull()?.let {
            return mapOf(USER_HDR_KEY to it.getName())
        }
        return null
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IrmCoreDataVaultClientImpl::class.java)
        /**
         * IRM expects this header to be set to the value of the current username.
         */
        const val USER_HDR_KEY = "imcUserId"
    }
}
