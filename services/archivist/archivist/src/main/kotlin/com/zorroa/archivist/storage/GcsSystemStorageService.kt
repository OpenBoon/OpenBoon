package com.zorroa.archivist.storage

import com.fasterxml.jackson.core.type.TypeReference
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.service.event
import com.zorroa.zmlp.util.Json
import java.nio.ByteBuffer
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("gcs")
class GcsSystemStorageService constructor(
    override val properties: SystemStorageProperties,
    val gcs: Storage

) : SystemStorageService {

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
