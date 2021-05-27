package boonai.archivist.service

import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelPublishRequest
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.storage.ProjectStorageService
import boonai.common.util.Json
import com.google.cloud.devtools.cloudbuild.v1.CloudBuildClient
import com.google.cloudbuild.v1.Build
import com.google.cloudbuild.v1.BuildStep
import com.google.cloudbuild.v1.CreateBuildRequest
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import org.springframework.core.io.support.ResourcePatternResolver
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.UUID

interface ModelDeployService {
    fun deployModelFileUpload(model: Model, inputStream: InputStream): PipelineMod
}

class ModelDeployServiceImpl(
    val fileStorageService: ProjectStorageService,
    val modelService: ModelService,
    val resourceResolver: ResourcePatternResolver

) {

    override fun deployModelFileUpload(model: Model, inputStream: InputStream): PipelineMod {
        if (!model.type.uploadable) {
            throw IllegalArgumentException("The model type ${model.type} does not support uploads")
        }

        /**
         * Stream the model into bucket storage.
         */
        val modelFile = ProjectStorageSpec(
            model.getModelStorageLocator("latest"), mapOf(),
            inputStream, 0L
        )
        fileStorageService.store(modelFile)

        /**
         * Store the version identifier.
         */
        val version = "${(System.currentTimeMillis() / 1000).toInt()}-${UUID.randomUUID()}\n"
        val versionBytes = version.toByteArray()
        val versionFile = ProjectStorageSpec(
            model.getModelVersionStorageLocator("latest"), mapOf(),
            ByteArrayInputStream(versionBytes), versionBytes.size.toLong()
        )
        fileStorageService.store(versionFile)


        modelService.publishModel(model, ModelPublishRequest(mapOf("version" to System.currentTimeMillis())))
    }

    fun emitMesssage(model: Model) : PubsubMessage {
        return PubsubMessage.newBuilder()
            .putAttributes("type", "model-upload")
            .putAttributes("modelId", model.id.toString())
            .putAttributes("projectId", model.projectId.toString())
            .putAttributes("service", model.id.toString().replace("-", ""))
            .putAttributes("image", model.id.toString().replace("-", ""))
            .build()
    }

}
