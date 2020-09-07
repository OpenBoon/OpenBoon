package com.zorroa.zmlp.service.storage

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.logging.warnEvent
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
        try {

            val data = Json.prettyString(any).toByteArray()
            val metadata = ObjectMetadata()
            metadata.contentType = "application/json"
            metadata.contentLength = data.size.toLong()

            s3Client.putObject(
                PutObjectRequest(
                    properties.bucket, path.trimStart('/'),
                    data.inputStream(),
                    metadata
                )
            )
            logger.event(LogObject.SYSTEM_STORAGE, LogAction.CREATE, mapOf("path" to path))
        } catch (e: Exception) {
            throw SystemStorageException("failed to store object $path", e)
        }
    }

    override fun <T> fetchObject(path: String, valueType: Class<T>): T {
        try {
            val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path.trimStart('/')))
            return Json.Mapper.readValue(s3obj.objectContent.readAllBytes(), valueType)
        } catch (e: Exception) {
            throw SystemStorageException("failed to fetch object $path", e)
        }
    }

    override fun <T> fetchObject(path: String, valueType: TypeReference<T>): T {
        try {
            val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path.trimStart('/')))
            return Json.Mapper.readValue(s3obj.objectContent.readAllBytes(), valueType)
        } catch (e: Exception) {
            throw SystemStorageException("failed to fetch object $path", e)
        }
    }

    override fun recursiveDelete(path: String) {
        logger.info("Recursive delete path:${properties.bucket}/$path")

        try {
            s3Client.listObjects(properties.bucket, path).objectSummaries.forEach {
                s3Client.deleteObject(properties.bucket, it.key)

                logger.event(
                    LogObject.SYSTEM_STORAGE, LogAction.DELETE,
                    mapOf(
                        "path" to path,
                        "key" to it.key
                    )
                )
            }
        } catch (ex: AmazonS3Exception) {
            logger.warnEvent(
                LogObject.PROJECT_STORAGE, LogAction.DELETE,
                "Failed to delete ${ex.message}",
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AwsSystemStorageService::class.java)
    }
}
