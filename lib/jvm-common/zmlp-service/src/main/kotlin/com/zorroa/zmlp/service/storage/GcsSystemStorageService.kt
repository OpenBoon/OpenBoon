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
import java.nio.ByteBuffer
import javax.annotation.PostConstruct

@Service
@Profile("gcs")
class GcsSystemStorageService constructor(
    override val properties: SystemStorageProperties

) : SystemStorageService {

    val gcs : Storage = StorageOptions.getDefaultInstance().service

    @PostConstruct
    fun initialize() {
        logger.info("Initializing GCS System Storage (bucket='${properties.bucket}')")
    }

    override fun storeObject(path: String, any: Any) {
        val blobId = BlobId.of(properties.bucket, path.trimStart('/'))
        val info = BlobInfo.newBuilder(blobId)
        gcs.writer(info.build()).write(ByteBuffer.wrap(Json.serialize(any)))
        logger.event(LogObject.SYSTEM_STORAGE, LogAction.CREATE, mapOf("path" to path))
    }

    override fun <T> fetchObject(path: String, valueType: Class<T>): T {
        val blobId = BlobId.of(properties.bucket, path.trimStart('/'))
        val blob = gcs.get(blobId)
        return Json.Mapper.readValue(blob.getContent(), valueType)
    }

    override fun <T> fetchObject(path: String, valueType: TypeReference<T>): T {
        val blobId = BlobId.of(properties.bucket, path.trimStart('/'))
        val blob = gcs.get(blobId)
        return Json.Mapper.readValue(blob.getContent(), valueType)
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsSystemStorageService::class.java)
    }
}
