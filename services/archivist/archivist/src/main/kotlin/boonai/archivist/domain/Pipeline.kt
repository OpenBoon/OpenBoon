package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Pipeline Mode", description = "Determines what mode the pipeline is in.")
enum class PipelineMode {
    @ApiModelProperty("Modular pipelines use pipeline-modules")
    MODULAR,

    @ApiModelProperty("Custom pipelines are manually built with processors")
    CUSTOM
}

@ApiModel("Pipeline", description = "The list of properties than be updated on a pipeline")
class PipelineUpdate(

    @ApiModelProperty("Name of the Pipeline.")
    var name: String,

    @ApiModelProperty("List of processors in this Pipeline.  Processors are ignored in modular pipelines")
    var processors: List<ProcessorRef>,

    @ApiModelProperty("All Pipeline modules applied to this pipeline. Modules are only used in modular pipelines.")
    var modules: List<UUID>
)

@ApiModel("Pipeline", description = "Describes a list of Processors for a Job to run in serial.")
class Pipeline(

    @ApiModelProperty("UUID of the Pipeline.")
    var id: UUID,

    @ApiModelProperty("The pipelines's project Ic.")
    var projectId: UUID,

    @ApiModelProperty("Name of the Pipeline.")
    var name: String,

    @ApiModelProperty("The mode the pipeline is in, either modular or custom.")
    var mode: PipelineMode,

    @ApiModelProperty("List of processors in this Pipeline.  Processors are ignored in modular pipelines")
    var processors: List<ProcessorRef>,

    @ApiModelProperty("All Pipeline modules applied to this pipeline. Modules are only used in modular pipelines.")
    var modules: List<UUID>,

    @ApiModelProperty("The time the Project was created..")
    val timeCreated: Long,

    @ApiModelProperty("The last time the Pipeline was modified.")
    var timeModified: Long,

    @ApiModelProperty("The actor which created this Project")
    var actorCreated: String,

    @ApiModelProperty("The actor that last made the last modification the project.")
    var actorModified: String

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

    @ApiModelProperty("The mode the pipeline is in, either modular or custom.")
    var mode: PipelineMode = PipelineMode.MODULAR,

    @ApiModelProperty("List of the Pipeline's processors, only used in Custom mode.")
    var processors: List<ProcessorRef>? = null,

    @ApiModelProperty("List of Pipeline Module names, only used in Modular mode.")
    var modules: List<String>? = null,

    @JsonIgnore
    @ApiModelProperty("List of the Pipeline's processors, only used in Modular mode.", hidden = true)
    var projectId: UUID? = null
)

@ApiModel("Pipeline Filter", description = "Search filter for finding Pipelines.")
data class PipelineFilter(

    @ApiModelProperty("Pipeline unique IDs.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Pipeline names.")
    val names: List<String>? = null,

    @ApiModelProperty("The pipeline mode.")
    val modes: List<PipelineMode>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "id" to "pipeline.pk_pipeline",
        "name" to "pipeline.str_name",
        "mode" to "pipeline.int_mode",
        "timeCreated" to "pipeline.time_created",
        "timeModified" to "pipeline.time_modified",
        "actorCreated" to "pipeline.actor_created",
        "actorModified" to "pipeline.actor_modified"
    )

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

        modes?.let {
            addToWhere(JdbcUtils.inClause("pipeline.int_mode", it.size))
            addToValues(it.map { mode -> mode.ordinal })
        }
    }
}
