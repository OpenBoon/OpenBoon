package com.zorroa.archivist.storage

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.DeleteObjectRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.cloud.storage.StorageException
import com.zorroa.archivist.domain.ArchivistException
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ProjectStorageLocator
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.zmlp.util.Json
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
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
        metadata.contentLength = spec.data.size.toLong()
        metadata.userMetadata = mapOf("attrs" to Json.serializeToString(spec.attrs))

        s3Client.putObject(
            PutObjectRequest(
                properties.bucket, path,
                spec.data.inputStream(), metadata
            )
        )

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
        val path = locator.getPath()
        val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))

        return try {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(s3obj.objectMetadata.contentType))
                .contentLength(s3obj.objectMetadata.contentLength)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(s3obj.objectContent))
        } catch (e: StorageException) {
            ResponseEntity.noContent().build()
        }
    }

    override fun fetch(locator: ProjectStorageLocator): ByteArray {
        val path = locator.getPath()
        val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))
        return s3obj.objectContent.readBytes()
    }

    override fun getNativeUri(locator: ProjectStorageLocator): String {
        return "s3://${properties.bucket}/${locator.getPath()}"
    }

    override fun delete(locator: ProjectStorageLocator) {
        try {
            val s3Object = s3Client.getObject(GetObjectRequest(properties.bucket, locator.getPath()))
            var obj = DeleteObjectRequest(properties.bucket, s3Object.key)
            s3Client.deleteObject(obj)
        } catch (ex: AmazonS3Exception) {
            throw ArchivistException(ex)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AwsProjectStorageService::class.java)
    }
}
