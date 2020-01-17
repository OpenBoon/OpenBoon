package com.zorroa.zmlp.service.storage

import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.util.Json
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("aws")
class AwsSystemStorageService constructor(
    override val properties: SystemStorageProperties
) : SystemStorageService {

    private val s3Client = getS3Client(properties)

    @PostConstruct
    fun initialize() {
        logger.info("Initializing AWS Storage Backend (bucket='${properties.bucket}')")
        if (properties.createBucket) {
            if (!s3Client.doesBucketExistV2(properties.bucket)) {
                s3Client.createBucket(properties.bucket)
            }
        }
    }

    override fun storeObject(path: String, any: Any) {
        val metadata = ObjectMetadata()
        s3Client.putObject(
            PutObjectRequest(
                properties.bucket, path.trimStart('/'),
                Json.prettyString(any).toByteArray().inputStream(),
                metadata
            )
        )
        logger.event(LogObject.SYSTEM_STORAGE, LogAction.CREATE, mapOf("path" to path))
    }

    override fun <T> fetchObject(path: String, valueType: Class<T>): T {
        val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path.trimStart('/')))
        return Json.Mapper.readValue(s3obj.objectContent.readAllBytes(), valueType)
    }

    override fun <T> fetchObject(path: String, valueType: TypeReference<T>): T {
        val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path.trimStart('/')))
        return Json.Mapper.readValue(s3obj.objectContent.readAllBytes(), valueType)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AwsSystemStorageService::class.java)
    }
}
