package com.zorroa.archivist.domain

import com.fasterxml.jackson.core.type.TypeReference
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
    @ApiModelProperty("The time the DataSource was created.")
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

)


@ApiModel("AddAssetToDataSetRequest", description = "Adds a list of labeled Assets to a DataSet")
class AddAssetsToDataSetRequest(
    val labels: List<AssetLabel>
)

class AssetLabel(
    val assetId: String,
    val label: String,
    val bbox: List<Float>?=null
)

class DataSetLabel(
    @ApiModelProperty("The ID of the DataSet")
    val dataSetId: UUID,
    @ApiModelProperty("The label for the DataSet")
    val label: String,
    @ApiModelProperty("An optional bounding box")
    val bbox: List<Float>?=null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataSetLabel) return false

        if (dataSetId != other.dataSetId) return false

        return true
    }

    override fun hashCode(): Int {
        return dataSetId.hashCode()
    }

    companion object {
        val SET_OF: TypeReference<MutableSet<DataSetLabel>> = object :
            TypeReference<MutableSet<DataSetLabel>>() {}
    }
}