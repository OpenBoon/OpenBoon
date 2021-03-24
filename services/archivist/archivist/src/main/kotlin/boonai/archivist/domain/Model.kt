package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import boonai.common.util.Json
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.math.BigDecimal
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
    val trainArgs: Map<String, Any>,
    val classifyProcessor: String,
    val classifyArgs: Map<String, Any>,
    val moduleName: String?,
    val description: String,
    val objective: String,
    val provider: String,
    val deployOnTrainingSet: Boolean,
    val minConcepts: Int,
    val minExamples: Int,
    val dependencies: List<String>,
    val trainable: Boolean,
    val uploadable: Boolean
) {
    KNN_CLASSIFIER(
        "K-Nearest Neighbors Classifier",
        "boonai_train.knn.KnnLabelDetectionTrainer",
        mapOf(),
        "boonai_analysis.custom.KnnLabelDetectionClassifier",
        mapOf(),
        null,
        "Classify images or documents using a KNN classifier.  This type of model generates " +
            "a single prediction which can be used to quickly organize assets into general groups." +
            "If no labels are provided, the model automatically generates numbered groups of " +
            "similar images. These groups can be renamed and edited in subsequent training passes.",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        true,
        0,
        0,
        listOf(),
        true,
        false
    ),
    TF_CLASSIFIER(
        "Tensorflow Transfer Learning Classifier",
        "boonai_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "train-test-ratio" to 4
        ),
        "boonai_analysis.custom.TensorflowTransferLearningClassifier",
        mapOf(),
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
        false
    ),
    FACE_RECOGNITION(
        "Face Recognition",
        "boonai_train.face_rec.KnnFaceRecognitionTrainer",
        mapOf(),
        "boonai_analysis.custom.KnnFaceRecognitionClassifier",
        mapOf(),
        "boonai-face-recognition",
        "Label faces detected by the boonai-face-detection module, and classify them with a KNN model. ",
        ModelObjective.FACE_RECOGNITION,
        Provider.BOONAI,
        true,
        1,
        1,
        listOf("boonai-face-detection"),
        true,
        false
    ),
    GCP_AUTOML_CLASSIFIER(
        "Google AutoML Classifier",
        "boonai_train.automl.AutoMLModelTrainer",
        mapOf(),
        "boonai_analysis.automl.AutoMLVisionClassifier",
        mapOf(),
        null,
        "Train an image classifier using Google Cloud AutoML.",
        ModelObjective.LABEL_DETECTION,
        Provider.GOOGLE,
        true,
        2,
        10,
        listOf(),
        true,
        false
    ),
    TF_UPLOADED_CLASSIFIER(
        "Imported Tensorflow Image Classifier",
        "None",
        mapOf(),
        "boonai_analysis.custom.TensorflowImageClassifier",
        mapOf(),
        null,
        "Upload a pre-trained Tensorflow model to use for image classification.",
        ModelObjective.LABEL_DETECTION,
        Provider.GOOGLE,
        true,
        0,
        0,
        listOf(),
        false,
        true
    ),
    PYTORCH_CLASSIFIER(
        "Pytorch Transfer Learning Classifier",
        "boonai_train.pytorch.PytorchTransferLearningTrainer",
        mapOf(
            "train-test-ratio" to 4
        ),
        "boonai_analysis.custom.PytorchTransferLearningClassifier",
        mapOf(),
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
        false
    ),
    PYTORCH_UPLOADED_CLASSIFIER(
        "Imported Pytorch Image Classifier",
        "None",
        mapOf(),
        "boonai_analysis.custom.PytorchTransferLearningClassifier",
        mapOf(),
        null,
        "Upload a pre-trained Pytorch model to use for image classification.",
        ModelObjective.LABEL_DETECTION,
        Provider.BOONAI,
        true,
        0,
        0,
        listOf(),
        false,
        true
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
            "label" to label
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

    @ApiModelProperty("Additional training args passed to processor.")
    var args: Map<String, Any>? = null,

    @ApiModelProperty("The action to take after training.")
    var postAction: PostTrainAction = PostTrainAction.NONE
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

    @ApiModelProperty("A model tag used to generate a PipelineMod name.")
    val moduleName: String? = null,

    @ApiModelProperty("The search used to deploy the model.")
    val applySearch: Map<String, Any> = ModelSearch.MATCH_ALL
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

    @Column(name = "int_type")
    val type: ModelType,

    @Column(name = "str_name")
    @ApiModelProperty("A name for the model, like 'bob's tree classifier'.")
    val name: String,

    @Column(name = "str_module")
    @ApiModelProperty("The name of the pipeline module and analysis namespace.")
    val moduleName: String,

    @Column(name = "str_file_id")
    val fileId: String,

    @Column(name = "str_job_name")
    val trainingJobName: String,

    @Column(name = "bool_trained")
    @ApiModelProperty("True if the model is trained.")
    val ready: Boolean,

    @Type(type = "jsonb")
    @Column(name = "json_apply_search", columnDefinition = "JSON")
    val applySearch: Map<String, Any>,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Model was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Model was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this Model")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this Model")
    val actorModified: String

) {
    @JsonIgnore
    fun getLabel(label: String, bbox: List<BigDecimal>? = null): Label {
        return Label(id, label, bbox = bbox)
    }

    fun getModelStorageLocator(tag: String): ProjectFileLocator {
        return ProjectFileLocator(
            ProjectStorageEntity.MODELS, id.toString(), tag, "model.zip"
        )
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
    }
}

@ApiModel("Model Filter", description = "A search filter for Models")
class ModelFilter(

    @ApiModelProperty("The Model IDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("The Model names to match")
    val names: List<String>? = null,

    @ApiModelProperty("The Model types to match")
    val types: List<ModelType>? = null

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

enum class LabelScope {
    TRAIN,
    TEST
}

object ModelSearch {

    val MATCH_ALL = mapOf<String, Any>("query" to mapOf("match_all" to emptyMap<String, Any>()))

    fun getTestSearch(model: Model): Map<String, Any> {
        return Json.Mapper.readValue(
            """
            {
                "bool": {
                    "filter": {
                        "nested" : {
                            "path": "labels",
                            "query" : {
                                "term": { 
                                    "labels.scope": "${LabelScope.TEST.name}" ,
                                    "labels.modelId": "${model.id}"
                                 }
                            }
                        }
                    }
                }
            }
        """,
            Json.GENERIC_MAP
        )
    }
}

@ApiModel("Label", description = "A Label which denotes a ground truth classification.")
class Label(
    @ApiModelProperty("The ID of the Model")
    val modelId: UUID,
    @ApiModelProperty("The label for the Asset")
    val label: String,
    @ApiModelProperty("The scope of the label.")
    val scope: LabelScope = LabelScope.TRAIN,
    bbox: List<BigDecimal>? = null,
    @ApiModelProperty("An an optional simhash for the label")
    val simhash: String? = null

) {

    @ApiModelProperty("An optional bounding box")
    val bbox: List<BigDecimal>? = bbox?.map { it.setScale(3, java.math.RoundingMode.HALF_UP) }

    companion object {
        val SET_OF: TypeReference<MutableSet<Label>> = object :
            TypeReference<MutableSet<Label>>() {}

        val LIST_OF: TypeReference<List<Label>> = object :
            TypeReference<List<Label>>() {}
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Label) return false

        if (modelId != other.modelId) return false
        if (bbox != other.bbox) return false
        return true
    }

    override fun hashCode(): Int {
        var result = modelId.hashCode()
        result = 31 * result + (bbox?.hashCode() ?: 0)
        return result
    }
}

@ApiModel("ModelApplyResponse", description = "The response to applying a model, either for testing or productions")
class ModelApplyResponse(

    @ApiModelProperty("Tbe number of Assets that will be processed.")
    val assetCount: Long,

    @ApiModelProperty("The ID of the job that is processing Assets.")
    val job: Job? = null
)

@ApiModel("Update Label Request", description = "Update or remove a given label.")
class UpdateLabelRequest(

    @ApiModelProperty("The name of the old label")
    val label: String,

    @ApiModelProperty("The name of the new label or null/empty string if the label should be removed.")
    val newLabel: String? = null
)
