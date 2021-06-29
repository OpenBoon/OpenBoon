package boonai.archivist.storage

import boonai.archivist.domain.Asset
import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.Dataset
import boonai.archivist.security.getAuthentication
import boonai.archivist.security.withAuth
import boonai.archivist.service.AssetService
import boonai.common.util.Json
import com.fasterxml.jackson.core.type.TypeReference
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import java.io.Closeable
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

interface BoonLibStorageService {
    /**
     * Copy a Map<src file, dst file> of files from one place to another.
     */
    fun copyFromProject(paths: Map<String, String>)

    /**
     * Store a file at the given path.
     */
    fun store(path: String, size: Long, stream: InputStream)

    /**
     * Stream a file using a ResponseEntity
     */
    fun stream(path: String): ResponseEntity<Resource>

    /**
     * Import assets into the given dataset.
     */
    fun importAssetsInto(boonLib: BoonLib, dataset: Dataset): BoonLibImportResponse
}

class BoonLibAssetImporter(
    val boonLib: BoonLib,
    val dataset: Dataset,
    val assetService: AssetService
) : Closeable {

    var total: Int = 0

    private var batch: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
    private val booonLibId = boonLib.id.toString()
    private val dataSetId = dataset.id.toString()

    private val auth = getAuthentication()
    private val executor = Executors.newFixedThreadPool(8)

    init {
        logger.info("Importing BoonLib ${boonLib.name} into dataset ${dataset.name}")
    }

    fun addAsset(bytes: ByteArray) {
        val asset = Json.Mapper.readValue(bytes, Asset::class.java)
        asset.setAttr("system.boonLibId", booonLibId)

        val labels = asset.getAttr("labels", LABEL_MAP)
        if (labels.isNullOrEmpty()) {
            logger.warn("The asset ${asset.id} has no labels")
            return
        }
        // Reset the dataSetId
        for (label in labels) {
            label["datasetId"] = dataSetId
        }

        asset.setAttr("labels", labels)
        batch[asset.id] = asset.document

        total += 1
        if (batch.size >= BATCH_SIZE) {
            launchBatch()
        }
    }

    override fun close() {
        if (batch.isNotEmpty()) {
            launchBatch()
        }
        try {
            executor.shutdown()
            executor.awaitTermination(10L, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    fun launchBatch() {
        val batchCopy = copyBatch()
        logger.info("Handing batch of ${batchCopy.size} assets")
        executor.execute {
            withAuth(auth) {
                assetService.batchIndex(batchCopy, setAnalyzed = false, refresh = false, create = true)
            }
        }
    }

    fun getCurrentBatch(): MutableMap<String, MutableMap<String, Any>> {
        return batch
    }

    fun copyBatch(): MutableMap<String, MutableMap<String, Any>> {
        val batchCopy = HashMap(batch)
        batch = mutableMapOf()
        return batchCopy
    }

    companion object {
        val LABEL_MAP: TypeReference<List<MutableMap<String, Any>>> =
            object : TypeReference<List<MutableMap<String, Any>>>() {}

        const val BATCH_SIZE = 128

        val logger = LoggerFactory.getLogger(BoonLibAssetImporter::class.java)
    }
}
