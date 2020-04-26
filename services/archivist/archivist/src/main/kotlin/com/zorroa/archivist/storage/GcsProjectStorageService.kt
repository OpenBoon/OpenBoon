package com.zorroa.archivist.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectStorageLocator
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.util.Json
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

    val gcs: Storage = StorageOptions.getDefaultInstance().service

    @PostConstruct
    fun initialize() {
        logger.info("Initializing GCS Storage Backend (bucket='${properties.bucket}')")
    }

    override fun store(spec: ProjectStorageSpec): FileStorage {
        val blobId = getBlobId(spec.locator)
        val info = BlobInfo.newBuilder(blobId)

        info.setMetadata(mapOf("attrs" to Json.serializeToString(spec.attrs)))
        info.setContentType(spec.mimetype)
        gcs.create(info.build(), spec.data)

        logStoreEvent(spec)

        return FileStorage(
            spec.locator.getFileId(),
            spec.locator.name,
            spec.locator.category,
            spec.mimetype,
            spec.data.size.toLong(),
            spec.attrs
        )
    }

    override fun stream(locator: ProjectStorageLocator): ResponseEntity<Resource> {
        val blobId = getBlobId(locator)
        val blob = gcs.get(blobId)

        return try {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(blob.contentType))
                .contentLength(blob.size)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(Channels.newInputStream(blob.reader())))
        } catch (e: StorageException) {
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

    override fun getSignedUrl(
        locator: ProjectStorageLocator,
        forWrite: Boolean,
        duration: Long,
        unit: TimeUnit
    ): String {
        val path = locator.getPath()
        val contentType = FileUtils.getMediaType(path)

        val info = BlobInfo.newBuilder(properties.bucket, path).setContentType(contentType).build()
        val opts = if (forWrite) {
            arrayOf(Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT))
        } else {
            arrayOf(Storage.SignUrlOption.httpMethod(HttpMethod.GET))
        }

        logSignEvent(path, forWrite)
        return gcs.signUrl(info, duration, unit, *opts).toString()
    }

    override fun setAttrs(locator: ProjectStorageLocator, attrs: Map<String, Any>): FileStorage {
        val path = locator.getPath()
        val info = BlobInfo.newBuilder(properties.bucket, path)
            .setMetadata(mapOf("attrs" to Json.serializeToString(attrs)))
            .build()

        return FileStorage(
            locator.getFileId(),
            locator.name,
            locator.category,
            info.contentType,
            info.size,
            attrs
        )
    }

    override fun recursiveDelete(locator: ProjectDirLocator) {
        val path = locator.getPath()
        val bucket = gcs.get(properties.bucket)
        val blobs = bucket.list(Storage.BlobListOption.prefix(path),
            Storage.BlobListOption.pageSize(100))

        for (blob in blobs.iterateAll()) {
            gcs.delete(blob.blobId)
            logDeleteEvent(blob.name)
        }
    }

    fun getBlobId(locator: ProjectStorageLocator): BlobId {
        return BlobId.of(properties.bucket, locator.getPath())
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsProjectStorageService::class.java)
    }
}
