package boonai.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
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
) {

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
