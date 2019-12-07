package com.zorroa.archivist.storage

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.util.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Configuration
@Profile("gcs")
class GcsFileStorageServiceConfiguration {

    @Bean
    fun getGcs(): Storage {
        return StorageOptions.getDefaultInstance().service
    }
}


@Service
@Profile("gcs")
class GcsFileStorageServiceImpl constructor(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService,
    val gcs: Storage

) : FileStorageService {

    @PostConstruct
    fun initialize() {
        FileStorageService.logger.info("Initializing GCS Storage Backend (bucket='${properties.bucket}')")
    }

    override fun store(spec: FileStorageSpec): FileStorage {

        val path = spec.locator.getPath()
        val blobId = BlobId.of(properties.bucket, path)
        val info = BlobInfo.newBuilder(blobId)

        info.setMetadata(mapOf("attrs" to Json.serializeToString(spec.attrs)))
        info.setContentType(spec.mimetype)
        gcs.writer(info.build()).write(ByteBuffer.wrap(spec.data))

        logStoreEvent(spec)

        return FileStorage(
            spec.locator.name,
            spec.locator.category.toString().toLowerCase(),
            spec.mimetype,
            spec.data.size.toLong(),
            spec.attrs
        )
    }

    override fun stream(locator: FileStorageLocator): ResponseEntity<Resource> {
        val path = locator.getPath()
        val blobId = BlobId.of(properties.bucket, path)
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

    override fun fetch(locator: FileStorageLocator): ByteArray {
        val path = locator.getPath()
        val blobId = BlobId.of(properties.bucket, path)
        val blob = gcs.get(blobId)
        return blob.getContent()
    }
}

