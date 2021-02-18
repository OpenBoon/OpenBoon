package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

class ProcessorSpec(
    val className: String,
    val type: String,
    val file: String,
    val fileTypes: List<String>?,
    val display: List<Map<String, Any>>?
)

@ApiModel("Processor", description = "Individual operation in a Pipeline.")
class Processor(

    @ApiModelProperty("UUID of the Processor.")
    val id: UUID,

    @ApiModelProperty("Dot-path to the location of the Processor python class.")
    val className: String,

    @ApiModelProperty("Type of this Processor.")
    val type: String,

    @ApiModelProperty("")
    val file: String,

    @ApiModelProperty("File types this processor supports.")
    val fileTypes: List<String>,

    @ApiModelProperty("")
    val display: List<Map<String, Any>>,

    @ApiModelProperty("Time this Processor was last updated.")
    val updatedTime: Long

)

@ApiModel("Processor Filter", description = "Search filter for finding Processors.")
class ProcessorFilter(

    @ApiModelProperty("Processor UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Types to match.")
    val types: List<String>? = null,

    @ApiModelProperty("Class names to match.")
    val classNames: List<String>? = null,

    @ApiModelProperty("File types to match.")
    var fileTypes: List<String>? = null,

    @ApiModelProperty("Returns a match if this string is found in the name or description of a processor.")
    var keywords: String? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "id" to "processor.pk_processor",
        "className" to "processor.str_name",
        "type" to "processor.str_type"
    )

    override fun build() {

        if (sort == null) {
            sort = listOf("className:a")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("processor.pk_processor", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("processor.str_type", it.size))
            addToValues(it)
        }

        classNames?.let {
            addToWhere(JdbcUtils.inClause("processor.str_name", it.size))
            addToValues(it)
        }

        fileTypes?.let {
            addToWhere(JdbcUtils.arrayOverlapClause("list_file_types", "text", it.size))
            addToValues(it)
        }

        keywords?.let {
            addToWhere("fti_keywords @@ to_tsquery(?)")
            addToValues(it)
        }
    }
}
