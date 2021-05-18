package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
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
 * Various DataSet types.
 */
enum class DataSetType {
    Classification,
    Detection,
    FaceRecognition
}

/**
 * Properties for a DataSet update.
 */
class DataSetUpdate(
    val name: String
)

/**
 * Properties for a new DataSet
 */
class DataSetSpec(
    val name: String,
    val type: DataSetType
)

/**
 * An interface attached to things which may contain labels, either
 * directly or indirectly.
 */
interface LabelSet {
    fun dataSetId(): UUID?
}

@Entity
@Table(name = "dataset")
@ApiModel("DataSet", description = "DataSets are groups of Assets.")
class DataSet(

    @Id
    @Column(name = "pk_dataset")
    @ApiModelProperty("The DataSet")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("A name for the DataSet.")
    var name: String,

    @Column(name = "int_type")
    val type: DataSetType,

    @Column(name = "time_created")
    @ApiModelProperty("The time the DataSet was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the DataSet was modified.")
    var timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this DataSet")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this DataSet")
    var actorModified: String
) : LabelSet {

    @JsonIgnore
    override fun dataSetId(): UUID? {
        return id
    }

    @JsonIgnore
    fun makeLabel(label: String, bbox: List<BigDecimal>? = null): Label {
        return Label(id, label, bbox = bbox)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataSet) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

enum class LabelScope {
    /**
     * Label is used for Training
     */
    Train,
    /**
     * Label is used for Testing
     */
    Test
}

@ApiModel("Label", description = "A Label which denotes a ground truth classification.")
class Label(
    @ApiModelProperty("The ID of the Model")
    val dataSetId: UUID,
    @ApiModelProperty("The label for the Asset")
    val label: String,
    @ApiModelProperty("The scope of the label.")
    val scope: LabelScope = LabelScope.Train,
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

        if (dataSetId != other.dataSetId) return false
        if (bbox != other.bbox) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dataSetId.hashCode()
        result = 31 * result + (bbox?.hashCode() ?: 0)
        return result
    }
}

class DataSetFilter(

    val ids: List<UUID>? = null,

    val names: List<String>? = null,

    val types: List<DataSetType>? = null

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
