package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.zmlp.util.Json
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
    val dependencies: List<String>
) {
    ZVI_KNN_CLASSIFIER(
        "zmlp_train.knn.KnnLabelDetectionTrainer",
        mapOf(),
        "zmlp_analysis.custom.KnnLabelDetectionClassifier",
        mapOf(),
        null,
        "Classify images or documents using a KNN classifier.  This type of model generates " +
            "a single prediction which can be used to quickly organize assets into general groups." +
            "The KNN classifier works with just a single image and label.",
        ModelObjective.LABEL_DETECTION,
        Provider.ZORROA,
        true,
        0,
        0,
        listOf()
    ),
    ZVI_LABEL_DETECTION(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "train-test-ratio" to 4
        ),
        "zmlp_analysis.custom.TensorflowTransferLearningClassifier",
        mapOf(),
        null,
        "Classify images or documents using a custom strained CNN deep learning algorithm.  This type of model" +
            "generates multiple predictions and can be trained to identify very specific features. " +
            "The label detection classifier requires at least 2 concepts with 10 labeled images each. ",
        ModelObjective.LABEL_DETECTION,
        Provider.ZORROA,
        false,
        2,
        10,
        listOf()
    ),
    ZVI_FACE_RECOGNITION(
        "zmlp_train.face_rec.KnnFaceRecognitionTrainer",
        mapOf(),
        "zmlp_analysis.custom.KnnFaceRecognitionClassifier",
        mapOf(),
        "zvi-face-recognition",
        "Relabel existing ZVI faces using a KNN Face Recognition model.",
        ModelObjective.FACE_RECOGNITION,
        Provider.ZORROA,
        true,
        1,
        1,
        listOf("zvi-face-detection")
    ),
    GCP_LABEL_DETECTION(
        "zmlp_train.automl.AutoMLModelTrainer",
        mapOf(),
        "zmlp_analysis.automl.AutoMLVisionClassifier",
        mapOf(),
        null,
        "Utilize Google AutoML to train an image classifier.",
        ModelObjective.LABEL_DETECTION,
        Provider.GOOGLE,
        true,
        2,
        10,
        listOf()
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
            "dependencies" to dependencies
        )
    }
}

@ApiModel("ModelTrainingArgs", description = "Arguments set to the training processor.")
class ModelTrainingArgs(

    @ApiModelProperty("Set to true if the model should be published, defaults to true.")
    val publish: Boolean = true,

    @ApiModelProperty("Deploy the model to production.")
    val deploy: Boolean = false,

    @ApiModelProperty("Additional training args passed to processor.")
    val args: Map<String, Any>? = null
)

@ApiModel("ModelApplyRequest", description = "Arguments set to the training processor.")
class ModelApplyRequest(

    @ApiModelProperty("A search to apply the model to. Defaults to the model deploy search.")
    val search: Map<String, Any>? = null,

    @ApiModelProperty("Don't filter the training set from the search.")
    val analyzeTrainingSet: Boolean = false,

    // TODO move
    @ApiModelProperty("Append the task to the given job, otherwise launch a new job.", hidden = true)
    val jobId: UUID? = null
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
    val deploySearch: Map<String, Any> = ModelSearch.MATCH_ALL
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
    @Column(name = "json_search_deploy", columnDefinition = "JSON")
    val deploySearch: Map<String, Any>,

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
