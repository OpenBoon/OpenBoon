package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.cloud.ServiceOptions
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * Type of models that can be trained.
 */
enum class ModelType(
    val label: String,
    val trainProcessor: String,
    val classifyProcessor: String,
    val moduleName: String?,
    val description: String,
    val objective: String,
    val provider: String,
    val deployOnTrainingSet: Boolean,
    val minConcepts: Int,
    val minExamples: Int,
    val dependencies: List<String>,
    val trainable: Boolean,
    val uploadable: Boolean,
    val enabled: Boolean,
    val datasetType: DatasetType,
    val fileName: String = "model.zip",
) {
    KNN_CLASSIFIER(
        "K-Nearest Neighbors Classifier",
        "boonai_train.knn.KnnLabelDetectionTrainer",
        "boonai_analysis.custom.KnnLabelDetectionClassifier",
        null,
        "Classify images, documents and video clips using a KNN classifier.  This type of model " +
            "can work great with just a single labeled example." +
            "If no labels are provided, the model automatically generates numbered groups of " +
            "similar assets. These groups can be renamed and edited in subsequent training passes.",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        true,
        0,
        0,
        listOf(),
        true,
        false,
        true,
        DatasetType.Classification
    ),
    TF_CLASSIFIER(
        "Tensorflow Transfer Learning Classifier",
        "boonai_train.tf2.TensorflowTransferLearningTrainer",
        "boonai_analysis.custom.TensorflowTransferLearningClassifier",
        null,
        "Classify images or documents using a custom strained CNN deep learning algorithm.  This type of model" +
            "generates multiple predictions and can be trained to identify very specific features. " +
            "The label detection classifier requires at least 2 concepts with 10 labeled images each. ",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        false,
        2,
        10,
        listOf(),
        true,
        false,
        true,
        DatasetType.Classification
    ),
    FACE_RECOGNITION(
        "Face Recognition",
        "boonai_train.face_rec.KnnFaceRecognitionTrainer",
        "boonai_analysis.custom.KnnFaceRecognitionClassifier",
        "boonai-face-recognition",
        "Label faces detected by the boonai-face-detection module, and classify them with a KNN model. ",
        ModelObjective.FACE_RECOGNITION,
        Provider.BOONAI,
        true,
        1,
        1,
        listOf("boonai-face-detection"),
        true,
        false,
        true,
        DatasetType.FaceRecognition
    ),
    GCP_AUTOML_CLASSIFIER(
        "Google AutoML Classifier",
        "boonai_train.automl.AutoMLModelTrainer",
        "boonai_analysis.automl.AutoMLVisionClassifier",
        null,
        "Train an image classifier using Google Cloud AutoML.",
        ModelObjective.LABEL_DETECTION,
        Provider.GOOGLE,
        true,
        2,
        10,
        listOf(),
        true,
        false,
        false,
        DatasetType.Classification
    ),
    TF_SAVED_MODEL_DISABLED(
        "Imported Tensorflow Image Classifier",
        "None",
        "boonai_analysis.custom.TensorflowImageClassifier",
        null,
        "Upload a pre-trained Tensorflow model to use for image classification.",
        ModelObjective.LABEL_DETECTION,
        Provider.GOOGLE,
        true,
        0,
        0,
        listOf(),
        false,
        true,
        false,
        DatasetType.Classification
    ),
    PYTORCH_DISABLED(
        "Pytorch Transfer Learning Classifier",
        "boonai_train.pytorch.PytorchTransferLearningTrainer",
        "boonai_analysis.custom.PytorchTransferLearningClassifier",
        null,
        "Classify images or documents using a custom trained CNN deep learning algorithm.  This type of model" +
            "generates multiple predictions and can be trained to identify very specific features. " +
            "The label detection classifier requires at least 2 concepts with 10 labeled images each. ",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        false,
        2,
        10,
        listOf(),
        true,
        false,
        false,
        DatasetType.Classification
    ),
    TORCH_MAR_CLASSIFIER(
        "A Torch Model Archive using the image_classifier handler.",
        "None",
        "boonai_analysis.custom.TorchModelArchiveClassifier",
        null,
        "Upload a pre-trained Pytorch Model Archive",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        true,
        0,
        0,
        listOf(),
        false,
        true,
        true,
        DatasetType.Classification,
        "model.mar"
    ),
    TORCH_MAR_DETECTOR(
        "A Torch Model Archive using the object_detector handler.",
        "None",
        "boonai_analysis.custom.TorchModelArchiveDetector",
        null,
        "Upload a pre-trained Pytorch Model Archive",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        true,
        0,
        0,
        listOf(),
        false,
        true,
        true,
        DatasetType.Detection,
        "model.mar"
    );

    fun asMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "description" to description,
            "objective" to objective,
            "provider" to provider,
            "deployOnTrainingSet" to deployOnTrainingSet,
            "minConcepts" to minConcepts,
            "minExamples" to minExamples,
            "dependencies" to dependencies,
            "label" to label,
            "datasetType" to datasetType.name
        )
    }
}

enum class PostTrainAction {
    /**
     * Don't do anything.
     */
    NONE,

    /**
     * Apply the model to the apply search.
     */
    APPLY,

    /**
     * Run on test labels only.
     */
    TEST,

    /**
     * Deploy
     */
    DEPLOY
}

@ApiModel("ModelTrainingArgs", description = "Arguments set to the training processor.")
class ModelTrainingRequest(

    @ApiModelProperty("The action to take after training.")
    var postAction: PostTrainAction = PostTrainAction.NONE,

    @ApiModelProperty("Override the default model training arguments.")
    var trainArgs: Map<String, Any>? = null
)

@ApiModel("ModelApplyRequest", description = "Arguments for applying a model to data.")
class ModelApplyRequest(

    @ApiModelProperty("A search to apply the model to. Defaults to the model apply search.")
    val search: Map<String, Any>? = null,

    @ApiModelProperty("Don't filter the training set from the search.")
    val analyzeTrainingSet: Boolean? = null,

    @ApiModelProperty("The version tag.")
    var tag: String = "latest"
)

@ApiModel("ModelPublishRequest", description = "Argument for publishing a model to an analysis module.")
class ModelPublishRequest(

    @ApiModelProperty("Arguments for the analysis processor.")
    var args: Map<String, Any> = emptyMap()
)

@ApiModel("ModelSpec", description = "Arguments required to create a new model")
class ModelSpec(

    @ApiModelProperty("The name of the model")
    val name: String,

    @ApiModelProperty("The type of mode")
    val type: ModelType,

    @ApiModelProperty("An associated Dataset")
    val datasetId: UUID? = null,

    @ApiModelProperty("A model tag used to generate a PipelineMod name.")
    val moduleName: String? = null,

    @ApiModelProperty("The search used to deploy the model.")
    val applySearch: Map<String, Any> = ModelSearch.MATCH_ALL,

    @ApiModelProperty("Training arguments")
    val trainingArgs: Map<String, Any> = emptyMap()
)

class ModelUpdateRequest(

    @ApiModelProperty("Name of the model")
    val name: String,

    @ApiModelProperty("The Dataset the model points to.")
    val datasetId: UUID?
)

class ModelPatchRequest(

    @ApiModelProperty("Name of the model")
    val name: String? = null,

    @ApiModelProperty("The Dataset the model points to.")
    val datasetId: UUID? = null
)

@Entity
@Table(name = "model")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
@ApiModel("Model", description = "Models are used to make predictions.")
class Model(

    @Id
    @Column(name = "pk_model")
    @ApiModelProperty("The unique ID of the Model")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "pk_dataset", nullable = true)
    var datasetId: UUID?,

    @Column(name = "int_type")
    val type: ModelType,

    @Column(name = "str_name")
    @ApiModelProperty("A name for the model, like 'bob's tree classifier'.")
    var name: String,

    @Column(name = "str_module")
    @ApiModelProperty("The name of the pipeline module and analysis namespace.")
    val moduleName: String,

    @Column(name = "str_endpoint")
    val endpoint: String?,

    @Column(name = "str_file_id")
    val fileId: String,

    @Column(name = "str_job_name")
    val trainingJobName: String,

    @Column(name = "bool_trained")
    @ApiModelProperty("True if the model is trained with latest labels.")
    var ready: Boolean,

    @Type(type = "jsonb")
    @Column(name = "json_apply_search", columnDefinition = "JSON")
    val applySearch: Map<String, Any>,

    @Type(type = "jsonb")
    @Column(name = "json_train_args", columnDefinition = "JSON")
    var trainingArgs: Map<String, Any>,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Model was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Model was modified.")
    var timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this Model")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this Model")
    var actorModified: String,

    @Column(name = "time_last_trained")
    var timeLastTrained: Long?,

    @Column(name = "actor_last_trained")
    var actorLastTrained: String?,

    @Column(name = "time_last_applied")
    var timeLastApplied: Long?,

    @Column(name = "actor_last_applied")
    var actorLastApplied: String?,

    @Column(name = "time_last_tested")
    var timeLastTested: Long?,

    @Column(name = "actor_last_tested")
    var actorLastTested: String?

) : LabelSet {

    @JsonIgnore
    override fun datasetId(): UUID? {
        return datasetId
    }

    @JsonIgnore
    fun imageName(): String {
        return "gcr.io/$GCP_PROJECT/models/$id"
    }

    fun getModelFileLocator(tag: String, name: String): ProjectFileLocator {
        return ProjectFileLocator(
            ProjectStorageEntity.MODELS, id.toString(), tag, name
        )
    }
    fun getModelStorageLocator(tag: String): ProjectFileLocator {
        return getModelFileLocator(tag, type.fileName)
    }

    fun getModelVersionStorageLocator(tag: String): ProjectFileLocator {
        return getModelFileLocator(tag, "model-version.txt")
    }

    @JsonIgnore
    fun getModuleName(tag: String? = null): String {
        return if (tag == null) {
            moduleName
        } else {
            "$moduleName:$tag"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Model) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    companion object {
        val matchAllSearch = mapOf<String, Any>("query" to mapOf("match_all" to emptyMap<String, Any>()))

        // Need this fo cloud container registry
        val GCP_PROJECT = ServiceOptions.getDefaultProjectId() ?: "localdev"
    }
}

@ApiModel("Model Filter", description = "A search filter for Models")
class ModelFilter(

    @ApiModelProperty("The Model IDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("The Model names to match")
    val names: List<String>? = null,

    @ApiModelProperty("The Model types to match")
    val types: List<ModelType>? = null,

    @ApiModelProperty("The Datasets assigned to model")
    val datasetIds: List<UUID>? = null

) : KDaoFilter() {
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "model.str_name",
        "timeCreated" to "model.time_created",
        "timeModified" to "model.time_modified",
        "id" to "model.pk_model",
        "type" to "model.int_type",
        "moduleName" to "model.str_module"
    )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        addToWhere("model.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("model.pk_model", it.size))
            addToValues(it)
        }

        datasetIds?.let {
            addToWhere(JdbcUtils.inClause("model.pk_dataset", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("model.str_name", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("model.int_type", it.size))
            addToValues(it.map { t -> t.ordinal })
        }
    }
}

object ModelSearch {

    val MATCH_ALL = mapOf<String, Any>("query" to mapOf("match_all" to emptyMap<String, Any>()))
}

@ApiModel("ModelApplyResponse", description = "The response to applying a model, either for testing or productions")
class ModelApplyResponse(

    @ApiModelProperty("Tbe number of Assets that will be processed.")
    val assetCount: Long,

    @ApiModelProperty("The ID of the job that is processing Assets.")
    val job: Job? = null
)

@ApiModel("ModelCopyRequest", description = "Request to copy a model from 1 tag to another.")
class ModelCopyRequest(

    @ApiModelProperty("The tag to copy")
    val srcTag: String,

    @ApiModelProperty("The destination tag")
    val dstTag: String
)
