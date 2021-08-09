package boonai.archivist.storage

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.Dataset
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.service.AssetService
import boonai.archivist.service.IndexRoutingService
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CopyObjectRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.common.base.Stopwatch
import kotlinx.coroutines.runBlocking
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Service
@Profile("aws")
class AwsBoonLibStorageService(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService,
    val s3Client: AmazonS3,
    val assetService: AssetService
) : BoonLibStorageService {

    val tika = Tika()

    override fun copyFromProject(paths: Map<String, String>) {
        val resolved = paths.map { ProjectFileLocator.buildPath(it.key) to it.value }.toMap()
        for (item in resolved) {
            val req = CopyObjectRequest(properties.bucket, item.key, properties.bucket, item.value)
            s3Client.copyObject(req)
        }
    }

    override fun store(path: String, size: Long, stream: InputStream) {
        val metadata = ObjectMetadata()
        val mediaType = tika.detect(path)

        metadata.contentLength = size
        metadata.contentType = mediaType
        s3Client.putObject(
            PutObjectRequest(
                properties.bucket, path,
                stream, metadata
            )
        )
    }

    override fun stream(path: String): ResponseEntity<Resource> {
        return try {
            val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(s3obj.objectMetadata.contentType))
                .contentLength(s3obj.objectMetadata.contentLength)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(s3obj.objectContent))
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }

    override fun importAssetsInto(boonLib: BoonLib, dataset: Dataset) = runBlocking {
        logger.info("Importing BoonLib ${boonLib.name} into dataset ${dataset.name}")

        boonLib.checkCompatible(dataset)

        val batcher = BoonLibAssetImporter(boonLib, dataset, assetService)
        val stopWatch = Stopwatch.createStarted()

        s3Client.listObjectsV2(properties.bucket, "boonlib/${boonLib.id}/").objectSummaries.forEach {
            if (it.key.endsWith("/asset.json")) {
                val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, it.key))
                s3obj.use { obj ->
                    batcher.addAsset(obj.objectContent.readAllBytes())
                }
            }
        }
        batcher.close()
        BoonLibImportResponse(batcher.total, stopWatch.elapsed(TimeUnit.MILLISECONDS))
    }

    companion object {
        val logger = LoggerFactory.getLogger(AwsBoonLibStorageService::class.java)
    }
}
