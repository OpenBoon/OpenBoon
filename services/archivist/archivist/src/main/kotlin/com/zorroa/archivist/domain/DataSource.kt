package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.util.JdbcUtils
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

    @ApiModelProperty("Override the project default pipeline with different.")
    val pipeline: String? = null
)

@ApiModel("DataSourceUodate", description = "Defines a DataSource fields that can be updated")
class DataSourceUpdate(

    @ApiModelProperty("The name of the DataSource")
    var name: String,

    @ApiModelProperty("The URI the DataSource points to.")
    var uri: String,

    @ApiModelProperty("A list of file extensions to filter", example = "[jpg,png]")
    @Convert(converter = StringListConverter::class)
    var fileTypes: List<String>?,

    @ApiModelProperty("Override the project default pipeline with different.")
    val pipelineId: UUID
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

    @ApiModelProperty("The Pipeline this DataSource is using.")
    @Column(name = "pk_pipeline")
    val pipelineId: UUID,

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

    fun getUpdated(update: DataSourceUpdate): DataSource {
        return DataSource(id,
            projectId,
            update.pipelineId,
            update.name,
            update.uri,
            update.fileTypes,
            timeCreated,
            System.currentTimeMillis(),
            actorCreated,
            getZmlpActor().toString())
    }

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

@ApiModel("DataSource Filter", description = "A search filter for DataSources")
class DataSourceFilter(

    /**
     * A list of unique Project IDs.
     */
    @ApiModelProperty("The DataSource IDs to match.")
    val ids: List<UUID>? = null,

    /**
     * A list of unique Project names.
     */
    @ApiModelProperty("The DataSource names to match")
    val names: List<String>? = null

) : KDaoFilter() {
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "datasource.str_name",
        "timeCreated" to "datasource.time_created",
        "timeModified" to "datasource.time_modified",
        "id" to "pk_datasource")

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        addToWhere("datasource.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("datasource.pk_datasource", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("datasource.str_name", it.size))
            addToValues(it)
        }
    }
}
