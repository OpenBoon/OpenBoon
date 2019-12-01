package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class ZpsSlot {
    Execute,
    Generate
}

@ApiModel("Pipeline", description = "Describes a list of Processors for a Job to run in serial.")
class Pipeline(

    @ApiModelProperty("UUID of the Pipeline.")
    var id: UUID,

    @ApiModelProperty("Name of the Pipeline.")
    var name: String,

    @ApiModelProperty("Type of the Pipeline.")
    var slot: ZpsSlot,

    @ApiModelProperty("List of processors in this Pipeline.")
    var processors: List<ProcessorRef> = mutableListOf()

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pipeline

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel("Pipeline Spec", description = "Attributes required to create a Pipeline.")
data class PipelineSpec(

    @ApiModelProperty("Name of the Pipeline.")
    var name: String,

    @ApiModelProperty("Type of the Pipeline.")
    var slot: ZpsSlot,

    @ApiModelProperty("List of the Pipeline's processors.")
    var processors: List<ProcessorRef> = mutableListOf()
)


@ApiModel("Pipeline Filter", description = "Search filter for finding Pipelines.")
data class PipelineFilter(

    @ApiModelProperty("Pipeline unique IDs.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Pipeline names.")
    val names: List<UUID>? = null,

    @ApiModelProperty("The Zps script slot the pipeline is used in")
    val slots: List<ZpsSlot>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "id" to "pipeline.pk_pipeline",
        "name" to "name.str_name")

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        addToWhere("pipeline.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("pipeline.pk_pipeline", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("pipeline.str_name", it.size))
            addToValues(it)
        }

        slots?.let {
            addToWhere(JdbcUtils.inClause("pipeline.int_slot", it.size))
            addToValues(it.map { s -> s.ordinal })
        }
    }
}

/**
 * TODO: replace with configuration file.
 *
 * This is the basic pipeline we're running for now until we
 * get into the Analysis dev phase.
 */
val STANDARD_PIPELINE = listOf(
    ProcessorRef("pixml_core.image.importers.ImageImporter", "zmlp-plugins-core")
)
