package com.zorroa.zmlp.service.storage

import com.fasterxml.jackson.core.type.TypeReference
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.util.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@Profile("gcs")
class GcsSystemStorageService constructor(
    override val properties: SystemStorageProperties

) : SystemStorageService {

    val gcs: Storage = StorageOptions.getDefaultInstance().service

    @PostConstruct
    fun initialize() {
        logger.info("Initializing GCS System Storage (bucket='${properties.bucket}')")
    }

    override fun storeObject(path: String, any: Any) {
        try {
            val blobId = BlobId.of(properties.bucket, path.trimStart('/'))
            val info = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
            gcs.create(info, Json.Mapper.writeValueAsBytes(any))
            logger.event(LogObject.SYSTEM_STORAGE, LogAction.CREATE, mapOf("path" to path))
        } catch (e: Exception) {
            throw SystemStorageException("failed to store object $path", e)
        }
    }

    override fun <T> fetchObject(path: String, valueType: Class<T>): T {
        try {
            val blobId = BlobId.of(properties.bucket, path.trimStart('/'))
            val blob = gcs.get(blobId)
            return Json.Mapper.readValue(blob.getContent(), valueType)
        } catch (e: Exception) {
            throw SystemStorageException("failed to fetch object $path", e)
        }
    }

    override fun <T> fetchObject(path: String, valueType: TypeReference<T>): T {
        try {
            val blobId = BlobId.of(properties.bucket, path.trimStart('/'))
            val blob = gcs.get(blobId)
            return Json.Mapper.readValue(blob.getContent(), valueType)
        } catch (e: Exception) {
            throw SystemStorageException("failed to fetch object $path", e)
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

            logger.event(
                LogObject.SYSTEM_STORAGE, LogAction.DELETE,
                mapOf(
                    "path" to path,
                    "blob_id" to blob.blobId
                )
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsSystemStorageService::class.java)
    }
}
