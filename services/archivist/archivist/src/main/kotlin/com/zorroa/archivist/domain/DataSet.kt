package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class DataSetType {
    LabelDetection,
    ObjectDetection,
    FaceRecognition
}

@ApiModel("Data Set", description = "Fields required to make a DataSet")
class DataSetSpec(

    @ApiModelProperty("A unique name for the DataSet")
    val name: String,

    @ApiModelProperty("The type of DataSet. The type determines what properties labels must have.")
    val type: DataSetType
)

@Entity
@Table(name = "data_set")
@ApiModel("Data Set", description = "A DataSet is used to train various classifiers.")
class DataSet(

    @Id
    @Column(name = "pk_data_set")
    @ApiModelProperty("The unique ID of the DataSet")
    val id: UUID,

    @Column(name = "pk_project")
    @ApiModelProperty("The unique ID of the Project.")
    val projectId: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("The unique name of the DataSet")
    val name: String,

    @Column(name = "int_type")
    @ApiModelProperty("The type of DataSet")
    val type: DataSetType,

    @Column(name = "time_created")
    @ApiModelProperty("The time the DataSet was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the DatSet was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this data set.")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this data set.")
    val actorModified: String

) {
    fun getLabel(label: String, bbox: List<Float>? = null): DataSetLabel {
        return DataSetLabel(id, label, bbox)
    }
}

class DataSetLabel(
    @ApiModelProperty("The ID of the DataSet")
    val dataSetId: UUID,
    @ApiModelProperty("The label for the DataSet")
    val label: String,
    @ApiModelProperty("An optional bounding box")
    val bbox: List<Float>? = null
) {
    companion object {
        val SET_OF: TypeReference<MutableSet<DataSetLabel>> = object :
            TypeReference<MutableSet<DataSetLabel>>() {}

        val LIST_OF: TypeReference<List<DataSetLabel>> = object :
            TypeReference<List<DataSetLabel>>() {}
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataSetLabel) return false

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

@ApiModel("DataSet Filter", description = "A search filter for DataSets")
class DataSetFilter(

    /**
     * A list of unique Project IDs.
     */
    @ApiModelProperty("The DataSet IDs to match.")
    val ids: List<UUID>? = null,

    /**
     * A list of unique Project names.
     */
    @ApiModelProperty("The DataSet names to match")
    val names: List<String>? = null,

    /**
     * A list of unique Project names.
     */
    @ApiModelProperty("The DataSet types to match")
    val types: List<DataSetType>? = null

) : KDaoFilter() {
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "data_set.str_name",
        "timeCreated" to "data_set.time_created",
        "timeModified" to "data_set.time_modified",
        "id" to "data_set.pk_data_set",
        "type" to "data_set.int_type")

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        addToWhere("data_set.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("data_set.pk_data_set", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("data_set.str_name", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("data_set.int_type", it.size))
            addToValues(it.map { t -> t.ordinal })
        }
    }
}
