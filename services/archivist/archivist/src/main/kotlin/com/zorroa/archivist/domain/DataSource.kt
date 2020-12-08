package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.FileTypeConverter
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.Table

@ApiModel("DataSourceSpec", description = "Defines a DataSource containing assets to import.")
class DataSourceSpec(

    @ApiModelProperty("The name of the DataSource")
    var name: String,

    @ApiModelProperty("The URI the DataSource points to.")
    var uri: String,

    @ApiModelProperty("An optional list of credentials blobs to populate import jobs.")
    var credentials: Set<String>? = null,

    @ApiModelProperty("A list of file extensions to filter", example = "[images, videos, documents]")
    var fileTypes: List<FileType> = FileType.allTypes(),

    @ApiModelProperty("Pipeline Modules for this DataSource.")
    var modules: Set<String>? = null
)

@ApiModel("DataSource Update", description = "Defines a DataSource fields that can be updated")
class DataSourceUpdate(

    @ApiModelProperty("The name of the DataSource")
    var name: String,

    @ApiModelProperty("The URI the DataSource points to.")
    var uri: String,

    /**
     * Using a List<String> here for now since old data sources will have file ext
     */
    @ApiModelProperty("A list of file extensions to filter", example = "[images,videos,documents]")
    var fileTypes: List<String>,

    @ApiModelProperty("An optional list of credentials blobs to populate import jobs.")
    var credentials: Set<String>?,

    @ApiModelProperty("Pipeline Modules for this DataSource.")
    val modules: Set<String>
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
    @Convert(converter = FileTypeConverter::class)
    @Column(name = "str_file_types")
    var fileTypes: List<FileType>,

    @ApiModelProperty("A list Credential IDS this datasource will use.")
    @ElementCollection
    @CollectionTable(name = "x_credentials_datasource", joinColumns = [JoinColumn(name = "pk_datasource")])
    @Column(name = "pk_credentials", insertable = false, updatable = false)
    var credentials: List<UUID>,

    @ElementCollection
    @CollectionTable(name = "x_module_datasource", joinColumns = [JoinColumn(name = "pk_datasource")])
    @Column(name = "pk_module", insertable = false, updatable = false)
    @ApiModelProperty("The Pipeline this DataSource is using.")
    val modules: List<UUID>,

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

) {

    fun getUpdated(update: DataSourceUpdate): DataSource {
        return DataSource(
            id,
            projectId,
            update.name,
            update.uri,
            FileType.fromArray(update.fileTypes),
            listOf(),
            listOf(),
            timeCreated,
            System.currentTimeMillis(),
            actorCreated,
            getZmlpActor().toString()
        )
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
        "id" to "pk_datasource"
    )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
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

@ApiModel("DataSource Import Options", description = "The options for importing a DataSource")
class DataSourceImportOptions(

    @ApiModelProperty("The number of Assets to include in a Task.")
    val batchSize: Int = 40
)

@ApiModel("DataSource Delete Options", description = "The options for Deleting a DataSource and its assets")
class DataSourceDelete(

    @ApiModelProperty("Should Delete Assets from Storage")
    val deleteAssets: Boolean = false
)
