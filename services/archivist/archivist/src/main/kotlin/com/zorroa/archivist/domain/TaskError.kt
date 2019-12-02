package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.repository.LongRangeFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

@ApiModel("Task Error", description = "Describes an error generated by an asset during processing. " +
    "TaskErrors are not cleared when an asset is re-processed cleanly, they persist until cleared or the job is cleared.")
class TaskError(

    @ApiModelProperty("UUID of this Task Error")
    val id: UUID,

    @ApiModelProperty("UUID of the Task that encountered an error.")
    val taskId: UUID,

    @ApiModelProperty("UUID of the Job the Task belongs to.")
    val jobId: UUID,

    @ApiModelProperty("The DataSource the error occurred on, if any.")
    val dataSourceId: UUID?,

    @ApiModelProperty("UUID of the Asset the Task was processing.")
    val assetId: String?,

    @ApiModelProperty("File path or URI that was being processed.")
    val path: String?,

    @ApiModelProperty("Error message from the exception that generated the error.")
    val message: String,

    @ApiModelProperty("Processor in which the error occurred.")
    val processor: String?,

    @ApiModelProperty("True is the error was fatal.")
    val fatal: Boolean,

    @ApiModelProperty("Hostname of the analyst the task was running on. The analyst ID is not used since they " +
        "cycle out quickly due to auto-scaling.")
    val analyst: String,

    @ApiModelProperty("Phase at which the error occurred: generate, execute, teardown.")
    val phase: String,

    @ApiModelProperty("Time a which the error was created in millis since epoch.")
    val timeCreated: Long,

    @ApiModelProperty("Full stack trace from the error, if any.")
    val stackTrace: List<StackTraceElement>? = null

)

@ApiModel("Stack Trace Element", description = "Describes a single line of a stack trace.")
class StackTraceElement(

    @ApiModelProperty("Name of the source file containing the execution point.")
    val file: String? = "Unknown File",

    @ApiModelProperty("Line number of the source line containing the execution point.")
    val lineNumber: Int = 0,

    @ApiModelProperty("Module or className of the source line containing the execution point.")
    val className: String? = "Unknown Class",

    @ApiModelProperty("Method containing the execution point.")
    val methodName: String? = "Unknown Method"

)

@ApiModel("Task Error Filter", description = "Search filter used to find Task Errors.")
class TaskErrorFilter(

    @ApiModelProperty("Task Error UUIDS to match.")
    var ids: List<UUID>? = null,

    @ApiModelProperty("Job UUIDS to match.")
    var jobIds: List<UUID>? = null,

    @ApiModelProperty("Task UUIDs to match.")
    var taskIds: List<UUID>? = null,

    @ApiModelProperty("Asset UUIDs to match.")
    var assetIds: List<String>? = null,

    @ApiModelProperty("DataSource UUIDs to match.")
    var dataSourceIds: List<UUID>? = null,

    @ApiModelProperty("Paths to match.")
    val paths: List<String>? = null,

    @ApiModelProperty("Processor names to match.")
    val processors: List<String>? = null,

    @ApiModelProperty("Limits results to Task Errors created within this range.")
    val timeCreated: LongRangeFilter? = null,

    @ApiModelProperty("Keyword query string.")
    val keywords: String? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
            "id" to "pk_task_error",
            "taskId" to "pk_task",
            "jobId" to "pk_job",
            "assetId" to "asset_id",
        "dataSourceId" to "job.pk_datasource",
            "path" to "str_path",
            "processors" to "str_processor",
            "timeCreated" to "time_created")

    @JsonIgnore
    override fun build() {

        if (sort == null) {
            sort = listOf("timeCreated:desc")
        }

        addToWhere("job.pk_project =?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_task_error", it.size))
            addToValues(it)
        }

        dataSourceIds?.let {
            addToWhere(JdbcUtils.inClause("job.pk_datasource", it.size))
            addToValues(it)
        }

        jobIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_job", it.size))
            addToValues(it)
        }

        taskIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_task", it.size))
            addToValues(it)
        }

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.asset_id", it.size))
            addToValues(it)
        }

        paths?.let {
            addToWhere(JdbcUtils.inClause("task_error.str_path", it.size))
            addToValues(it)
        }

        processors?.let {
            addToWhere(JdbcUtils.inClause("task_error.str_processor", it.size))
            addToValues(it)
        }

        timeCreated?.let {
            addToWhere(JdbcUtils.rangeClause("task_error.time_created", it))
            addToValues(it.getFilterValues())
        }

        keywords?.let {
            addToWhere("fti_keywords @@ to_tsquery(?)")
            addToValues(it)
        }
    }
}
