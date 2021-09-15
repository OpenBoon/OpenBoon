package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import boonai.common.util.Json
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.math.BigDecimal
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * Various Dataset types.
 */
enum class DatasetType(
    val label: String,
    val description: String
) {
    Classification(
        "Classification",
        "Use when classifying the asset."
    ),
    Detection(
        "Detection",
        "Use when labeling objects within assets."
    ),
    FaceRecognition(
        "Face Recognition",
        "Use when labeling detected faces."
    );

    fun asMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "label" to label,
            "description" to description
        )
    }
}

/**
 * Properties for a Dataset update.
 */
class DatasetUpdate(
    val name: String,
    val description: String
)

/**
 * Properties for a new Dataset
 */
class DatasetSpec(
    val name: String,
    val type: DatasetType,
    var description: String = ""
)

/**
 * An interface attached to things which may contain labels, either
 * directly or indirectly.
 */
interface LabelSet {
    fun datasetId(): UUID?
}

@Entity
@Table(name = "dataset")
@ApiModel("Dataset", description = "Datasets are groups of Assets.")
class Dataset(

    @Id
    @Column(name = "pk_dataset")
    @ApiModelProperty("The Dataset")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("A name for the Dataset.")
    var name: String,

    @Column(name = "int_type")
    val type: DatasetType,

    @Column(name = "str_descr")
    var description: String,

    @Column(name = "int_model_count")
    val modelCount: Int,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Dataset was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Dataset was modified.")
    var timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this Dataset")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this Dataset")
    var actorModified: String
) : LabelSet {

    @JsonIgnore
    override fun datasetId(): UUID? {
        return id
    }

    @JsonIgnore
    fun makeLabel(label: String, bbox: List<BigDecimal>? = null, simhash: String? = null): Label {
        return Label(id, label, bbox = bbox, simhash = simhash)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Dataset) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    @JsonIgnore
    fun getAssetSearch(): Map<String, Any> {
        val json =
            """
            {
                "bool": {
                    "must": [
                        {
                            "nested" : {
                                "path": "labels",
                                "query" : {
                                    "bool": {
                                        "filter": [
                                            {"term": { "labels.datasetId": "$id"}}
                                        ]
                                    }
                                }
                            }
                        }
                    ]
                }
            }
            """.trimIndent()

        return mapOf("query" to Json.Mapper.readValue(json, Json.GENERIC_MAP))
    }
}

enum class LabelScope {
    /**
     * Label is used for Training
     */
    TRAIN,
    /**
     * Label is used for Testing
     */
    TEST
}

@ApiModel("Update Label Request", description = "Update or remove a given label.")
class UpdateLabelRequest(

    @ApiModelProperty("The name of the old label")
    val label: String,

    @ApiModelProperty("The name of the new label or null/empty string if the label should be removed.")
    val newLabel: String? = null
)

/**
 * The response for when adding a label to an Asset.
 */
enum class LabelResult {
    /**
     * The label was created.
     */
    Created,

    /**
     * The label was updated.
     */
    Updated,

    /**
     * The label was a duplicate.
     */
    Duplicate
}

@ApiModel("Label", description = "A Label which denotes a ground truth classification.")
class Label(
    @ApiModelProperty("The ID of the Model")
    var datasetId: UUID,
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

        if (datasetId != other.datasetId) return false
        if (bbox != other.bbox) return false
        return true
    }

    override fun hashCode(): Int {
        var result = datasetId.hashCode()
        result = 31 * result + (bbox?.hashCode() ?: 0)
        return result
    }
}

class DatasetFilter(

    val ids: List<UUID>? = null,

    val names: List<String>? = null,

    val types: List<DatasetType>? = null

) : KDaoFilter() {
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "dataset.str_name",
        "timeCreated" to "dataset.time_created",
        "timeModified" to "dataset.time_modified",
        "id" to "dataset.pk_model",
        "type" to "dataset.int_type"
    )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        addToWhere("dataset.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("dataset.pk_dataset", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("dataset.str_name", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("dataset.int_type", it.size))
            addToValues(it.map { t -> t.ordinal })
        }
    }
}
