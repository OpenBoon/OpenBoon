package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.util.StringListConverter
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@ApiModel("DataSourceSpec", description = "Defines a DataSource containing assets to import.")
class DataSourceSpec(

    @ApiModelProperty("The name of the DataSource")
    var name: String,

    @ApiModelProperty("The URI the DataSource points to.")
    var uri: String,

    @ApiModelProperty("An optional credentials blob for the DataSource, this will be encrypted.")
    var credentials: String? = null,

    @ApiModelProperty("A list of file extensions to filter", example = "[jpg,png]")
    @Convert(converter = StringListConverter::class)
    var fileTypes: List<String>? = null,

    @ApiModelProperty(
        "A list of analysis modules to apply to the DataSource, " +
            "this overrides project defaults if set."
    )
    @Convert(converter = StringListConverter::class)
    var analysis: List<String>? = null
)

@Entity
@Table(name = "datasource")
@ApiModel("Data Source", description = "A DataSource describes a URI where Assets can be imported from.")
class DataSource(
    @Id
    @Column(name = "pk_datasource")
    @ApiModelProperty("The Unique ID of the DataSource")
    val id: UUID,

    @Column(name = "pk_project")
    @ApiModelProperty("The Unique ID of the Project.")
    val projectId: UUID,

    @Column(name = "str_name")
    @ApiModelProperty("The unique name of the DataSource")
    val name: String,

    @Column(name = "str_uri")
    @ApiModelProperty("The URI of the DataSource")
    val uri: String,

    @ApiModelProperty("A list of file type filters.")
    @Convert(converter = StringListConverter::class)
    @Column(name = "str_file_types")
    var fileTypes: List<String>?,

    @ApiModelProperty("The default Analysis modules for this data source")
    @Convert(converter = StringListConverter::class)
    @Column(name = "str_analysis")
    var analysis: List<String>?,

    @Column(name = "time_created")
    @ApiModelProperty("The time the datasource was created.")
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataSource

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel(
    "DataSourceCredentials",
    description = "A DataSource credentials blob."
)
class DataSourceCredentials(

    @ApiModelProperty("A credentials blob of some kind. See docs for more details.")
    var blob: String? = null,

    @JsonIgnore
    @ApiModelProperty("The SALT used to encrypt the credentials.", hidden = true)
    var salt: String? = null
)