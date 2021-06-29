package boonai.archivist.storage

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.Dataset
import boonai.archivist.service.AssetService
import boonai.archivist.util.loadGcpCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.common.base.Stopwatch
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit

@Service
@Profile("gcs")
class GcsBoonLibStorageService(
    val properties: StorageProperties,
    val assetService: AssetService
) : BoonLibStorageService {

    val options: StorageOptions = StorageOptions.newBuilder()
        .setCredentials(loadGcpCredentials()).build()
    val gcs: Storage = options.service

    override fun copyFromProject(paths: Map<String, String>) {
        for (item in paths) {
            val srcBlob = gcs.get(properties.bucket, item.key)
            srcBlob.copyTo(properties.bucket, item.value)
        }
    }

    override fun store(path: String, size: Long, stream: InputStream) {
        val blobId = getBlobId(path)
        val info = BlobInfo.newBuilder(blobId)
        gcs.createFrom(info.build(), stream)
    }

    override fun stream(path: String): ResponseEntity<Resource> {
        val blobId = getBlobId(path)

        return try {
            val blob = gcs.get(blobId)
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(blob.contentType))
                .contentLength(blob.size)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(Channels.newInputStream(blob.reader())))
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }

    override fun importAssetsInto(boonLib: BoonLib, dataset: Dataset) = runBlocking {
        val batcher = BoonLibAssetImporter(boonLib, dataset, assetService)
        val stopWatch = Stopwatch.createStarted()

        val bucket = gcs.get(properties.bucket)
        val blobs = bucket.list(Storage.BlobListOption.prefix("boonlib/${boonLib.id}/"))

        for (blob in blobs.iterateAll()) {
            if (blob.blobId.name.endsWith("/asset.json")) {
                batcher.addAsset(gcs.readAllBytes(blob.blobId))
            }
        }

        batcher.close()
        BoonLibImportResponse(batcher.total, stopWatch.elapsed(TimeUnit.MILLISECONDS))
    }

    fun getBlobId(path: String): BlobId {
        return BlobId.of(properties.bucket, path)
    }
}
