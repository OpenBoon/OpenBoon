package boonai.archivist.service

import boonai.archivist.domain.Asset
import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelPublishRequest
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.PubSubEvent
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.loadGcpCredentials
import boonai.archivist.util.randomString
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
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.pubsub.v1.PubsubMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.UUID
import javax.annotation.PostConstruct

interface ModelDeployService {
    /**
     * Deploys an uploaded model file to an endpoint.  The model file is uploaded
     * to cloud storage.
     */
    fun deployUploadedModel(model: Model, inputStream: InputStream)
}

@Service
class ModelDeployServiceImpl(
    val fileStorageService: ProjectStorageService,
    val modelService: ModelService,
    val eventBus: EventBus

) : ModelDeployService {

    @PostConstruct
    fun setup() {
        eventBus.register(this)
    }

    override fun deployUploadedModel(model: Model, inputStream: InputStream) {
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
        fileStorageService.store(modelFile)

        /**
         * Store the version identifier.
         */
        val version = "${(System.currentTimeMillis() / 1000).toInt()}-${randomString(8)}"
        val versionBytes = version.plus("\n").toByteArray()
        val versionFile = ProjectStorageSpec(
            model.getModelVersionStorageLocator("latest"), mapOf(),
            ByteArrayInputStream(versionBytes), versionBytes.size.toLong()
        )
        fileStorageService.store(versionFile)

        // Emit a message to signal for the model to be deployed.
        modelService.postToModelEventTopic(buildDeployPubsubMessage(model))
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
            val model = modelService.getModel(modelId)

            val endpoint = findCloudRunEndpoint(model)
            // TODO: update the model.endpoint property, make a specific function
            // in ModelJdbcDaoImpl for this and don't allow normal users to
            // update the endpoint.

            // Now make the module available.
            modelService.publishModel(model, ModelPublishRequest())
        }
    }

    fun buildDeployPubsubMessage(model: Model): PubsubMessage {
        val loc = model.getModelStorageLocator("latest")

        return PubsubMessage.newBuilder()
            .putAttributes("type", "model-upload")
            .putAttributes("modelId", model.id.toString())
            .putAttributes("modelType", model.type.name)
            .putAttributes("modelFile", fileStorageService.getNativeUri(loc))
            .putAttributes("projectId", model.projectId.toString())
            .putAttributes("image", model.imageName())
            .build()
    }

    /**
     * Makes a REST request to a cloud run endpoint which allow us to get the
     * cloud run endpoint URL for a particular custom model.
     */
    fun findCloudRunEndpoint(model: Model): String? {

        // Archivist credentials at "/secrets/gcs/credentials.json" in production.
        val credsPath = "/Users/chambers/src/zmlp/containers/plugins-analysis/integration_tests/google_integration_tests/gcp-creds.json"
        var credentials = loadGcpCredentials()

        // This will be: ServiceOptions.getDefaultProjectId()
        val projectId = "zvi-dev"
        val modelId = model.id
        val url = "https://us-central1-run.googleapis.com/apis/serving.knative.dev/v1/namespaces/$projectId/services/$modelId?alt=json"

        // TODO: Does anything have to be closed here?
        val credentialsAdapter = HttpCredentialsAdapter(credentials)
        val requestFactory: HttpRequestFactory = NetHttpTransport().createRequestFactory(credentialsAdapter)
        val request: HttpRequest = requestFactory.buildGetRequest(GenericUrl(url))
        val response: HttpResponse = request.execute()

        val data = Asset(Json.Mapper.readValue(response.content, Json.GENERIC_MAP).toMutableMap())
        return data.getAttr("status.url", String::class.java)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ModelDeployServiceImpl::class.java)
    }
}
