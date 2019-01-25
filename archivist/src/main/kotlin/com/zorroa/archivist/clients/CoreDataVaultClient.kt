package com.zorroa.common.clients

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.security.getUserOrNull
import com.zorroa.archivist.service.event
import com.zorroa.archivist.service.warnEvent
import com.zorroa.common.util.Json
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

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
     * Return true if the asset exists.
     * @param companyId the company id
     * @param assetId the asset Id
     * @return Boolean true if asset exists
     */
    fun assetExists(companyId: Int, assetId: String) : Boolean

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
     * Will not throw any exceptions.
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

    /**
     * Upload the source media to the given URI
     *
     * @param uri: the file upload url returned from the call to create the asset.
     * @param bytes: the array of bytes representing the file.
     */
    fun uploadSource(uri: URI, bytes: ByteArray)

}

class IrmCoreDataVaultClientImpl constructor(url: String, serviceKey: Path, dataKey: Path) : CoreDataVaultClient {

    override val client = RestClient(url, GcpJwtSigner(serviceKey))

    private val gcs: Storage

    init {
        gcs = if (Files.exists(dataKey)) {
            StorageOptions.newBuilder()
                    .setCredentials(
                            GoogleCredentials.fromStream(FileInputStream(dataKey.toFile()))).build().service
        }
        else {
            StorageOptions.newBuilder().build().service
        }

        logger.info("Initialized CDV REST client $url")

    }

    override fun assetExists(companyId: Int, assetId: String) : Boolean {
        return try {
            getAsset(companyId, assetId)
            true
        }
        catch (e: RestClientException) {
            false
        }
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
            val updated = response["status"] == "PASSED"
            if (updated) {
                client.put("/companies/$companyId/documents/$assetId/fields/state/INDEXED",
                        null, Json.GENERIC_MAP, headers = getRequestHeaders())
            }
            updated
        } catch (e: Exception) {
            logger.warnEvent(LogObject.ASSET, LogAction.UPDATE, e.message ?: "No error message",
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

    override fun uploadSource(uri: URI, bytes: ByteArray) {
        val (bucket, path) = uri.path.substring(1).split('/', limit=2)
        val blobId = BlobId.of(bucket, path)
        gcs.create(BlobInfo.newBuilder(blobId).build(), bytes)
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
