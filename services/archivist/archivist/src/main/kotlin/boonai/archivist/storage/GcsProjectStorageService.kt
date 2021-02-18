package boonai.archivist.storage

import com.google.api.client.util.ByteStreams
import com.google.auth.oauth2.ComputeEngineCredentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.ProjectDirLocator
import boonai.archivist.domain.ProjectStorageLocator
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.service.IndexRoutingService
import boonai.archivist.util.FileUtils
import boonai.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Service
@Profile("gcs")
class GcsProjectStorageService constructor(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService

) : ProjectStorageService {

    val options: StorageOptions = StorageOptions.newBuilder()
        .setCredentials(loadCredentials()).build()
    val gcs: Storage = options.service

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

        gcs.writer(info.build()).use {
            ByteStreams.copy(
                spec.stream,
                Channels.newOutputStream(it)
            )
        }

        logStoreEvent(spec)

        return FileStorage(
            spec.locator.getFileId(),
            spec.locator.name,
            spec.locator.category,
            spec.mimetype,
            spec.size.toLong(),
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
        val blobs = bucket.list(
            Storage.BlobListOption.prefix(path),
            Storage.BlobListOption.pageSize(100)
        )

        for (blob in blobs.iterateAll()) {
            gcs.delete(blob.blobId)
            logDeleteEvent(blob.name)
        }
    }

    fun getBlobId(locator: ProjectStorageLocator): BlobId {
        return BlobId.of(properties.bucket, locator.getPath())
    }

    private fun loadCredentials(): GoogleCredentials {
        val credsFile = Paths.get("/secrets/gcs/credentials.json")

        return if (Files.exists(credsFile)) {
            logger.info("Loading credentials from: {}", credsFile)
            GoogleCredentials.fromStream(FileInputStream(credsFile.toFile()))
        } else {
            logger.info("Using default ComputeEngineCredentials")
            ComputeEngineCredentials.create()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsProjectStorageService::class.java)
    }
}
