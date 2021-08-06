package boonai.archivist.storage

import com.amazonaws.ClientConfiguration
import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.ProjectDirLocator
import boonai.archivist.domain.ProjectStorageLocator
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.service.IndexRoutingService
import boonai.archivist.util.FileUtils
import boonai.archivist.util.randomString
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.warnEvent
import boonai.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.CopyObjectResult
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Configuration
@Profile("aws")
class AwsStorageConfiguration(val properties: StorageProperties) {

    @Bean
    fun getS3Client(): AmazonS3 {

        val credentials: AWSCredentials = BasicAWSCredentials(
            properties.accessKey, properties.secretKey
        )

        val clientConfiguration = ClientConfiguration()
        clientConfiguration.signerOverride = "AWSS3V4SignerType"
        clientConfiguration.maxConnections = 100
        clientConfiguration.connectionTimeout = 30 * 1000

        return AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(
                EndpointConfiguration(
                    properties.url, Regions.DEFAULT_REGION.name
                )
            )
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .build()
    }
}

@Service
@Profile("aws")
class AwsProjectStorageService constructor(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService,
    val s3Client: AmazonS3
) : ProjectStorageService {

    @PostConstruct
    fun initialize() {
        logger.info("Initializing AWS Storage Backend (bucket='${properties.bucket}')")
        if (properties.createBucket) {
            if (!s3Client.doesBucketExistV2(properties.bucket)) {
                s3Client.createBucket(properties.bucket)
            }
        }
    }

    override fun store(spec: ProjectStorageSpec): FileStorage {

        val path = spec.locator.getPath()
        val metadata = ObjectMetadata()
        metadata.contentType = spec.mimetype
        metadata.userMetadata = mapOf("attrs" to Json.serializeToString(spec.attrs))

        if (spec.stream is FileInputStream) {
            metadata.contentLength = spec.stream.channel.size()
            s3Client.putObject(
                PutObjectRequest(
                    properties.bucket, path,
                    spec.stream, metadata
                )
            )
        } else {
            // Need to do for large files
            val tmpFile = Files.createTempFile(randomString(32), "data")
            try {
                Files.copy(spec.stream, tmpFile, StandardCopyOption.REPLACE_EXISTING)
                metadata.contentLength = Files.size(tmpFile)

                s3Client.putObject(
                    PutObjectRequest(
                        properties.bucket, path,
                        FileInputStream(tmpFile.toFile()), metadata
                    )
                )
            } finally {
                Files.delete(tmpFile)
            }
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
        val path = locator.getPath()

        return try {
            val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(s3obj.objectMetadata.contentType))
                .contentLength(s3obj.objectMetadata.instanceLength)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(s3obj.objectContent))
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }

    override fun streamLogs(locator: ProjectStorageLocator): ResponseEntity<Resource> {
        return this.stream(locator)
    }

    override fun fetch(locator: ProjectStorageLocator): ByteArray {
        val path = locator.getPath()

        try {
            val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))
            s3obj.use {
                return it.objectContent.readBytes()
            }
        } catch (ex: AmazonS3Exception) {
            throw ProjectStorageException("Failed to fetch $path", ex)
        }
    }

    override fun copy(src: String, dst: String): Long {
        val req = CopyObjectRequest(properties.bucket, src, properties.bucket, dst)
        val res: CopyObjectResult = s3Client.copyObject(req)
        return res.lastModifiedDate.time
    }

    override fun getNativeUri(locator: ProjectStorageLocator): String {
        return "s3://${properties.bucket}/${locator.getPath()}"
    }

    override fun getNativeUri(locator: ProjectDirLocator): String {
        return "s3://${properties.bucket}/${locator.getPath()}"
    }

    override fun getSignedUrl(
        locator: ProjectStorageLocator,
        forWrite: Boolean,
        duration: Long,
        unit: TimeUnit
    ): Map<String, Any> {
        val path = locator.getPath()
        val mediaType = FileUtils.getMediaType(path)
        val expireTime = Date(System.currentTimeMillis() + unit.toMillis(duration))
        val method = if (forWrite) {
            HttpMethod.PUT
        } else {
            HttpMethod.GET
        }

        val req: GeneratePresignedUrlRequest =
            GeneratePresignedUrlRequest(properties.bucket, path)
                .withMethod(method)
                .withExpiration(expireTime)

        logSignEvent(path, mediaType, forWrite)
        return mapOf(
            "uri" to
                s3Client.generatePresignedUrl(req).toString(),
            "mediaType" to mediaType
        )
    }

    override fun setAttrs(locator: ProjectStorageLocator, attrs: Map<String, Any>): FileStorage {
        val path = locator.getPath()
        val metadata = s3Client.getObjectMetadata(properties.bucket, path)

        return FileStorage(
            locator.getFileId(),
            locator.name,
            locator.category,
            metadata.contentType,
            metadata.contentLength,
            attrs
        )
    }

    override fun recursiveDelete(locator: ProjectDirLocator) {
        val path = locator.getPath()
        logger.info("Recursive delete path:${properties.bucket}/$path")

        try {
            s3Client.listObjects(properties.bucket, path).objectSummaries.forEach {
                s3Client.deleteObject(properties.bucket, it.key)
                logDeleteEvent("${properties.bucket}${it.key}")
            }
        } catch (ex: AmazonS3Exception) {
            logger.warnEvent(
                LogObject.PROJECT_STORAGE, LogAction.DELETE,
                "Failed to delete ${ex.message}",
                mapOf("entityId" to locator.entityId, "entity" to locator.entity)
            )
        }
    }

    override fun listFiles(prefix: String): List<String> {
        return s3Client.listObjects(properties.bucket, prefix).objectSummaries.map { it.key }
    }

    override fun recursiveDelete(path: String) {
        logger.info("Recursive delete path:${properties.bucket}/$path")

        try {
            s3Client.listObjects(properties.bucket, path).objectSummaries.forEach {
                s3Client.deleteObject(properties.bucket, it.key)
                logDeleteEvent("${properties.bucket}${it.key}")
            }
        } catch (ex: AmazonS3Exception) {
            logger.warnEvent(
                LogObject.PROJECT_STORAGE, LogAction.DELETE,
                "Failed to delete ${ex.message}",
                mapOf("path" to path)
            )
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AwsProjectStorageService::class.java)
    }
}
