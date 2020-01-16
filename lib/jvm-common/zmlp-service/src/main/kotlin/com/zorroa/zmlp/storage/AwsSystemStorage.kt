package com.zorroa.zmlp.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.zmlp.logging.LogAction
import com.zorroa.zmlp.logging.LogObject
import com.zorroa.zmlp.logging.event
import com.zorroa.zmlp.util.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@Profile("aws")
class AwsSystemStorageServiceImpl constructor(
    override val properties: SystemStorageProperties,
    val s3Client: AmazonS3
) : SystemStorageService {

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
        val logger = LoggerFactory.getLogger(AwsSystemStorageServiceImpl::class.java)
    }
}