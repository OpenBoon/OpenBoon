package com.zorroa.archivist.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectStorageLocator
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.IndexRoutingService
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
            spec.locator.category.toLowerCase(),
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

    override fun recursiveDelete(locator: ProjectDirLocator) {
        val path = locator.getPath()
        gcs.list(path).values.forEach {
            gcs.delete(it.blobId)
        }
        logDeleteEvent(path)
    }

    fun getBlobId(locator: ProjectStorageLocator): BlobId {
        return BlobId.of(properties.bucket, locator.getPath())
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsProjectStorageService::class.java)
    }
}
