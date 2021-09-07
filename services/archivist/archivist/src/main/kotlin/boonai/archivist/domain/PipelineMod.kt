package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.repository.PipelineModDaoImpl
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("PipelineMod", description = "A ZMLP Pipeline Modifier")
class PipelineMod(

    @ApiModelProperty("The Unique ID of the module")
    val id: UUID,

    @ApiModelProperty("The project the module is associated with or Null for all projects")
    val projectId: UUID?,

    @ApiModelProperty("A unique name of the module")
    val name: String,

    @ApiModelProperty("A description of what the module does")
    val description: String,

    @ApiModelProperty("The provider or maintainer of module.")
    val provider: String,

    @ApiModelProperty("The service or type of ML, eg \"Google Vision\")")
    val category: String,

    @ApiModelProperty("The objective of the module.")
    val type: String,

    @ApiModelProperty("The general types of media this module can handle.")
    val supportedMedia: List<String>,

    @ApiModelProperty("A list of operations to apply to the pipeline")
    val ops: List<ModOp>,

    @ApiModelProperty("The time the Pipeline Mod was created..")
    val timeCreated: Long,

    @ApiModelProperty("The last time the Pipeline Mod was modified.")
    val timeModified: Long,

    @ApiModelProperty("The actor which created this Pipeline Mod")
    val actorCreated: String,

    @ApiModelProperty("The actor that last made the last modification the Pipeline Mod.")
    val actorModified: String
) {

    /**
     * Used internally for pipeline resolution.
     */
    @JsonIgnore
    var force: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PipelineMod) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel("PipelineModFilter", description = "Used to search for PipelineMods.")
class PipelineModFilter(

    @ApiModelProperty("The mod IDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("The mod names to match")
    val names: List<String>? = null,

    @ApiModelProperty("The mod type to match")
    val types: List<String>? = null,

    @ApiModelProperty("The categories type to match")
    val categories: List<String>? = null,

    @ApiModelProperty("The providers type to match")
    val providers: List<String>? = null

) : KDaoFilter() {

    override val sortMap: Map<String, String> =
        mapOf(
            "name" to "module.str_name",
            "timeCreated" to "module.time_created",
            "timeModified" to "module.time_modified",
            "id" to "module.pk_module",
            "type" to "module.str_type",
            "provider" to "module.str_provider",
            "category" to "module.str_category"
        )

    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        // Filters by our project ID or standard.
        addToWhere(PipelineModDaoImpl.PROJ_FILTER)
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("module.pk_module", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("module.str_name", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("module.str_type", it.size))
            addToValues(it)
        }

        providers?.let {
            addToWhere(JdbcUtils.inClause("module.str_provider", it.size))
            addToValues(it)
        }

        categories?.let {
            addToWhere(JdbcUtils.inClause("module.str_category", it.size))
            addToValues(it)
        }
    }
}

@ApiModel("PipelineModUpdate", description = "All the Pipeline mod fields that can be updated.")
class PipelineModUpdate(

    @ApiModelProperty("A unique name of the module")
    val name: String,

    @ApiModelProperty("A description of what the module does")
    val description: String,

    @ApiModelProperty("The provider or maintainer of module.")
    val provider: String,

    @ApiModelProperty("The provider's service or product used for the module, eg \"Google Vision\")")
    val category: String,

    @ApiModelProperty("The underlying technology type?")
    val type: String,

    @ApiModelProperty("The types of media this module can handle.")
    val supportedMedia: List<FileType>,

    @ApiModelProperty("A list of operations to apply to the pipeline")
    val ops: List<ModOp>
)

@ApiModel("PipelineModSpec", description = "Fields for creating a new Pipeline Mod")
class PipelineModSpec(

    @ApiModelProperty("A unique name of the module")
    var name: String,

    @ApiModelProperty("A description of what the module does")
    val description: String,

    @ApiModelProperty("The provider or maintainer of module.")
    val provider: String,

    @ApiModelProperty("The provider's service or product used for the module, eg \"Google Vision\")")
    val category: String,

    @ApiModelProperty("The underlying technology type?")
    val type: String,

    @ApiModelProperty("The types of media this module can handle.")
    val supportedMedia: List<FileType>,

    @ApiModelProperty("A list of operations to apply to the pipeline")
    val ops: List<ModOp>,

    @ApiModelProperty("Determines if this is a Standard or Project level mod.", hidden = true)
    val standard: Boolean = false
)

@ApiModel("Pipeline ModOpType", description = "Types of operations a Pipeline Mod can make.")
enum class ModOpType() {

    @ApiModelProperty("Set arguments on a ProcessorRef")
    SET_ARGS,
    @ApiModelProperty("Append to the Pipeline")
    APPEND,
    @ApiModelProperty("Prepend to the Pipeline")
    PREPEND,
    @ApiModelProperty("Add processors before a matched processor")
    ADD_BEFORE,
    @ApiModelProperty("Add processors after a matched processor")
    ADD_AFTER,
    @ApiModelProperty("Replace matched processor")
    REPLACE,
    @ApiModelProperty("Remove matched processors")
    REMOVE,
    @ApiModelProperty("Append processors as far back as possible.")
    LAST,
    @ApiModelProperty("Append once and only once, merge args if any.")
    APPEND_MERGE,
    @ApiModelProperty("Load dependent pipeline modules.")
    DEPEND
}

@ApiModel("Pipeline Mod Operation", description = "An operation to apply to a Pipeline")
class ModOp(
    @ApiModelProperty("The opType")
    val type: ModOpType,
    @ApiModelProperty("The data necessary for OpType, which depends on the type")
    val apply: Any?,
    @ApiModelProperty("Matchers which determine if an Op is applied.")
    var filter: OpFilter? = null,
    @ApiModelProperty("The max number of times the Op should be applied")
    val maxApplyCount: Int = 1
) {
    /**
     * This is incremented every time an Op is applied.
     */
    @JsonIgnore
    var applyCount: Int = 0
}

@ApiModel("Filter Type", description = "The type of Filter.")
enum class OpFilterType {
    @ApiModelProperty("Match processor names with a regular expression.")
    REGEX,
    @ApiModelProperty("Match processor names with a substr match.")
    SUBSTR,
    @ApiModelProperty("Match processor names with equals.")
    EQUAL,
    @ApiModelProperty("Match processor names that don't match this regex.")
    NOT_REGEX,
    @ApiModelProperty("Match processor names that don't match this substr.")
    NOT_SUBSTR
}

@ApiModel("Op Filter", description = "Matches processor names and args on a ProcessorRef")
class OpFilter(
    @ApiModelProperty("The Filter type")
    val type: OpFilterType,
    @ApiModelProperty("The text to Match")
    val processor: String?
)
