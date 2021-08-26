package boonai.archivist.storage

import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.ProjectDirLocator
import boonai.archivist.domain.ProjectStorageLocator
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.service.IndexRoutingService
import boonai.archivist.util.FileUtils
import boonai.archivist.util.loadGcpCredentials
import boonai.common.util.Json
import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.CopyWriter
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Service
@Profile("gcs")
class GcsProjectStorageService constructor(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService

) : ProjectStorageService {

    val options: StorageOptions = StorageOptions.newBuilder()
        .setCredentials(loadGcpCredentials()).build()
    val gcs: Storage = options.service

    val loggingService: Logging = LoggingOptions.newBuilder().build().service

    @PostConstruct
    fun initialize() {
        logger.info(
            "Initializing GCS Storage Backend (bucket='${properties.bucket}')"
        )
    }

    override fun store(spec: ProjectStorageSpec): FileStorage {
        val blobId = getBlobId(spec.locator)
        val info = BlobInfo.newBuilder(blobId)

        info.setMetadata(mapOf("attrs" to Json.serializeToString(spec.attrs)))
        info.setContentType(spec.mimetype)

        gcs.createFrom(info.build(), spec.stream)
        logStoreEvent(spec)

        return FileStorage(
            spec.locator.getFileId(),
            spec.locator.name,
            spec.locator.category,
            spec.mimetype,
            spec.size,
            spec.attrs
        )
    }

    override fun stream(locator: ProjectStorageLocator): ResponseEntity<Resource> {
        val blobId = getBlobId(locator)

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

    override fun streamLogs(locator: ProjectStorageLocator): ResponseEntity<Resource> {

        val blob = gcs.get(getBlobId(locator))

        // Retrieve logs from file in bucket
        return if (blob.exists()) {
            this.stream(locator)
        } else {
            ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(InputStreamResource(GcpLogInputStream(loggingService, locator)))
        }
    }

    override fun copy(src: String, dst: String): Long {
        val srcBlob = gcs.get(properties.bucket, src)
        val copyWriter: CopyWriter = srcBlob.copyTo(properties.bucket, dst)
        return copyWriter.result.updateTime
    }

    override fun fetch(locator: ProjectStorageLocator): ByteArray {
        val blobId = getBlobId(locator)
        val blob = gcs.get(blobId)
        return blob.getContent()
    }

    override fun getNativeUri(locator: ProjectStorageLocator): String {
        val path = locator.getPath()
        return "gs://${properties.bucket}/$path"
    }

    override fun getNativeUri(locator: ProjectDirLocator): String {
        val path = locator.getPath()
        return "gs://${properties.bucket}/$path"
    }

    override fun getSignedUrl(
        locator: ProjectStorageLocator,
        forWrite: Boolean,
        duration: Long,
        unit: TimeUnit
    ): Map<String, Any> {
        val path = locator.getPath()
        val mediaType = FileUtils.getMediaType(path)

        val info = BlobInfo.newBuilder(properties.bucket, path).setContentType(mediaType).build()
        val opts = if (forWrite) {
            arrayOf(
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT)
            )
        } else {
            arrayOf(
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
            )
        }

        logSignEvent(path, mediaType, forWrite)
        return mapOf(
            "uri" to gcs.signUrl(info, duration, unit, *opts).toString(),
            "mediaType" to mediaType
        )
    }

    override fun setAttrs(locator: ProjectStorageLocator, attrs: Map<String, Any>): FileStorage {
        val path = locator.getPath()
        val mediaType = FileUtils.getMediaType(path)
        val info = BlobInfo.newBuilder(properties.bucket, path)
            .setContentType(mediaType)
            .setMetadata(mapOf("attrs" to Json.serializeToString(attrs)))
            .build()

        val blob = gcs.update(info)

        return FileStorage(
            locator.getFileId(),
            locator.name,
            locator.category,
            mediaType,
            blob.size,
            attrs
        )
    }

    override fun recursiveDelete(locator: ProjectDirLocator) {
        val path = locator.getPath()
        val bucket = gcs.get(properties.bucket)
        val blobs = bucket.list(
            Storage.BlobListOption.prefix(path),
            Storage.BlobListOption.pageSize(100)
        )

        for (blob in blobs.iterateAll()) {
            gcs.delete(blob.blobId)
            logDeleteEvent(blob.name)
        }
    }

    override fun recursiveDelete(path: String) {
        val bucket = gcs.get(properties.bucket)

        var attempt = 0
        val maxNumberOfAttempts = 10

        while (attempt < maxNumberOfAttempts) {
            val blobs = bucket.list(
                Storage.BlobListOption.prefix(path),
            )

            logDeleteEvent(path)
            val storageFileId: List<List<BlobId>> = blobs.iterateAll().map { it.blobId }.chunked(10000)
            var success = true

            storageFileId.forEachIndexed { index, element ->
                logDeleteEvent(path, index + 1, storageFileId.size)
                success = success && gcs.delete(element).all { it }
            }

            // Check if everything was deleted
            if (!success) {
                attempt += 1
                logger.warn("Something went wrong on GCS deleting $path. Retry $attempt of $maxNumberOfAttempts")
            } else {
                logger.info("Path completely deleted from GCS: $path")
                break
            }
        }
    }

    override fun listFiles(prefix: String): List<String> {
        val bucket = gcs.get(properties.bucket)
        val blobs = bucket.list(
            Storage.BlobListOption.prefix(prefix),
        )
        return blobs.iterateAll().map { it.name }
    }

    fun getBlobId(locator: ProjectStorageLocator): BlobId {
        return BlobId.of(properties.bucket, locator.getPath())
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsProjectStorageService::class.java)
    }
}
