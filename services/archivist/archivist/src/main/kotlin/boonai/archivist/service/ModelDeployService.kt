package boonai.archivist.service

import boonai.archivist.domain.Asset
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelPublishRequest
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.PubSubEvent
import boonai.archivist.repository.ModelDao
import boonai.archivist.repository.ModelJdbcDao
import boonai.archivist.security.InternalThreadAuthentication
import boonai.archivist.security.withAuth
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.loadGcpCredentials
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.util.Json
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestFactory
import com.google.api.client.http.HttpResponse
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.auth.http.HttpCredentialsAdapter
import com.google.cloud.ServiceOptions
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.pubsub.v1.PubsubMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

interface ModelDeployService {
    /**
     * Deploys an uploaded model file to an endpoint.  The model file is uploaded
     * to cloud storage.
     */
    fun deployUploadedModel(model: Model, inputStream: InputStream): FileStorage
    fun getSignedModelUploadUrl(model: Model): Map<String, Any>
    fun kickoffModelBuild(model: Model)
}

@Service
class ModelDeployServiceImpl(
    val fileStorageService: ProjectStorageService,
    val modelService: ModelService,
    val modelJdbcDao: ModelJdbcDao,
    val modelDao: ModelDao,
    val eventBus: EventBus
) : ModelDeployService {

    @PostConstruct
    fun setup() {
        eventBus.register(this)
    }

    override fun getSignedModelUploadUrl(model: Model): Map<String, Any> {
        if (!model.type.uploadable) {
            throw IllegalArgumentException("This type of model cannot be uploaded")
        }
        return fileStorageService.getSignedUrl(
            model.getModelStorageLocator("latest"), true, 10L, TimeUnit.MINUTES
        )
    }

    override fun kickoffModelBuild(model: Model) {
        if (!model.type.uploadable) {
            throw IllegalArgumentException("This type of model cannot be uploaded")
        }
        modelService.postToModelEventTopic(buildDeployPubsubMessage(model))
    }

    override fun deployUploadedModel(model: Model, inputStream: InputStream): FileStorage {
        if (!model.type.uploadable) {
            throw IllegalArgumentException("The model type ${model.type} does not support uploads")
        }

        logger.event(
            LogObject.MODEL, LogAction.UPLOAD,
            mapOf("modelId" to model.id, "modelName" to model.name, "image" to model.imageName())
        )

        /**
         * Store the uploaded model file.
         */
        val modelFile = ProjectStorageSpec(
            model.getModelStorageLocator("latest"), mapOf(),
            inputStream, 0
        )
        val fs = fileStorageService.store(modelFile)

        // Emit a message to signal for the model to be deployed.
        modelService.postToModelEventTopic(buildDeployPubsubMessage(model))

        return fs
    }

    /**
     * Handles pubsub messages related to successful cloud builds that have
     * deployed a model.
     */
    @Subscribe
    fun handleModelBuildEvent(event: PubSubEvent) {
        if (!event.attrs.containsKey("buildId")) {
            return
        }

        /**
         * Status types
         * QUEUED, WORKING, FAILURE, SUCCESS
         * Only handling success right now which publishes module.
         */
        val status = event.attrs["status"]
        if (status == "SUCCESS") {
            val doc = Asset(
                Json.Mapper.readValue(event.data.toByteArray(), Json.GENERIC_MAP).toMutableMap()
            )

            val images = doc.getAttr("images", Json.LIST_OF_STRING)
            if (images.isNullOrEmpty()) {
                logger.warn("Model build has no images property")
                return
            }

            val image = images[0]
            if (!image.contains("/models/")) {
                // The build is not for a model image.
                return
            }

            val modelId = UUID.fromString(image.split("/").last())
            logger.info("Deploying uploaded model $modelId")
            val model = modelDao.getOne(modelId)

            val endpoint = findCloudRunEndpoint(model)
            if (endpoint != null) {
                val auth = InternalThreadAuthentication(model.projectId)
                withAuth(auth) {
                    logger.info("Setting ${model.id} endpoint to $endpoint")
                    modelJdbcDao.setEndpoint(model.id, endpoint)
                    modelService.publishModel(model, ModelPublishRequest(mapOf("endpoint" to endpoint)))
                }
            } else {
                logger.error("The model build ${model.id} completed but no endpoint was found.")
            }
        }
    }

    fun buildDeployPubsubMessage(model: Model): PubsubMessage {
        val loc = model.getModelStorageLocator("latest")

        return PubsubMessage.newBuilder()
            .putAttributes("type", "model-upload")
            .putAttributes("modelId", model.id.toString())
            .putAttributes("modelType", model.type.name)
            .putAttributes(
                "modelFile",
                fileStorageService.getSignedUrl(loc, false, 24, TimeUnit.HOURS)["uri"].toString()
            )
            .putAttributes("projectId", model.projectId.toString())
            .putAttributes("image", model.imageName())
            .build()
    }

    /**
     * Makes a REST request to a cloud run endpoint which allow us to get the
     * cloud run endpoint URL for a particular custom model.
     */
    fun findCloudRunEndpoint(model: Model): String? {

        val projectId = ServiceOptions.getDefaultProjectId()
        val modelId = model.id
        val url = "https://us-central1-run.googleapis.com/apis/serving.knative.dev/v1/namespaces/$projectId/services/mod-$modelId?alt=json"

        val credentialsAdapter = HttpCredentialsAdapter(credentials)
        val requestFactory: HttpRequestFactory = NetHttpTransport().createRequestFactory(credentialsAdapter)
        val request: HttpRequest = requestFactory.buildGetRequest(GenericUrl(url))
        val response: HttpResponse = request.execute()

        // Closes the stream
        response.content.use {
            val data = Asset(Json.Mapper.readValue(it, Json.GENERIC_MAP).toMutableMap())
            return data.getAttr("status.url", String::class.java)
        }
    }

    companion object {

        val credentials = loadGcpCredentials()

        private val logger = LoggerFactory.getLogger(ModelDeployServiceImpl::class.java)
    }
}
