package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.UserBase
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import io.micrometer.core.instrument.Tag
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class JobState {
    Active,
    Cancelled,
    Finished,
    Archived;

    /**
     * Return a Micrometer tag for tagging metrics related to this state.
     */
    fun metricsTag(): Tag {
        return Tag.of("job-state", this.toString())
    }
}

interface JobId {
    val jobId: UUID
}

/**
 * Standard Job priority values. A lower priority goes first.
 *
 * @property Standard Standard job priority.
 * @property Interactive Interactive job priority.
 * @property Reindex Reindex job priority.
 */
object JobPriority {
    const val Standard = 100
    const val Interactive = 1
    const val Reindex = -32000
}

@ApiModel("Job Spec", description = "Attributes required to create a Job.")
class JobSpec(

    @ApiModelProperty("Name of the job")
    var name: String?,

    @ApiModelProperty("ZPS Script this Job will run.")
    var script: ZpsScript?,

    @ApiModelProperty("Args to run the ZPS script with.")
    val args: MutableMap<String, Any>? = mutableMapOf(),

    @ApiModelProperty("Environment to pass to the ZPS script.")
    val env: MutableMap<String, String>? = mutableMapOf(),

    @ApiModelProperty("Priority of the Job.")
    var priority: Int = JobPriority.Standard,

    @ApiModelProperty("If true create the Job in the paused state.")
    var paused: Boolean = false,

    @ApiModelProperty("Number of seconds to keep the Job paused after creation.")
    val pauseDurationSeconds: Long? = null,

    @ApiModelProperty("If true replace Jobs of the same name.")
    val replace: Boolean = false,

    @ApiModelProperty("The maximum number of running tasks on the job.")
    val maxRunningTasks: Int = 1024

)

@ApiModel("Job Update Spec", description = "Attributes required to update a Job.")
class JobUpdateSpec(

    @ApiModelProperty("Name of the Job.")
    var name: String,

    @ApiModelProperty("Priority for the Job.")
    val priority: Int,

    @ApiModelProperty("If true the Job will be paused.")
    val paused: Boolean,

    @ApiModelProperty("Time the Job should be unpaused.")
    val timePauseExpired: Long,

    @ApiModelProperty("The maximum number of running tasks on the job.")
    val maxRunningTasks: Int
)

@ApiModel("Job", description = "Describes an Analyst Job.")
class Job(

    @ApiModelProperty("UUID of the Job.")
    val id: UUID,

    @ApiModelProperty("UUID of the Organization this Job belongs to.")
    val organizationId: UUID,

    @ApiModelProperty("Name of the Job.")
    val name: String,

    @ApiModelProperty("Type of Pipeline this Job will run.")
    val type: PipelineType,

    @ApiModelProperty("Current state of this Job.")
    val state: JobState,

    @ApiModelProperty("Number of Assets this Job is running.")
    var assetCounts: Map<String, Int>? = null,

    @ApiModelProperty("Number of Tasks in this Job.")
    var taskCounts: Map<String, Int>? = null,

    @ApiModelProperty("Time the Job was started.")
    var timeStarted: Long,

    @ApiModelProperty("Time the Job was last updated.")
    var timeUpdated: Long,

    @ApiModelProperty("Time the Job was created.")
    var timeCreated: Long,

    @ApiModelProperty("Current priority of the Job,")
    var priority: Int,

    @ApiModelProperty("If true this Job is paused.")
    val paused: Boolean,

    @ApiModelProperty("Time this Job will unpause itself.")
    val timePauseExpired: Long,

    @ApiModelProperty("The maximum number of running tasks on the job.")
    val maxRunningTasks: Int

) : JobId {
    override val jobId = id

    @JsonIgnore
    fun getStorageId(): String {
        return "job___$jobId"
    }
}

@ApiModel("Job Filter", description = "Search filter for finding Jobs.")
class JobFilter(

    @ApiModelProperty("Job UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Pipeline Type to match.")
    val type: PipelineType? = null,

    @ApiModelProperty("States to match.")
    val states: List<JobState>? = null,

    @ApiModelProperty("Organization UUIDs to match.")
    val organizationIds: List<UUID>? = null,

    @ApiModelProperty("Job names to match.")
    val names: List<String>? = null,

    @ApiModelProperty("Paused status to match.")
    val paused: Boolean? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("id" to "job.pk_job",
                    "type" to "job.int_type",
                    "name" to "job.str_name",
                    "timeCreated" to "job.time_created",
                    "state" to "job.int_state",
                    "priority" to "job.int_priority",
                    "organizationId" to "job.pk_organization")

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        if (hasPermission("zorroa::superadmin")) {
            organizationIds?.let {
                addToWhere(JdbcUtils.inClause("job.pk_organization", it.size))
                addToValues(it)
            }
        } else {
            addToWhere("job.pk_organization=?")
            addToValues(getOrgId())
        }

        if (!hasPermission("zorroa::admin")) {
            addToWhere("job.pk_user_created=?")
            addToValues(getUserId())
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("job.pk_job", it.size))
            addToValues(it)
        }

        if (type != null) {
            addToWhere("job.int_Type=?")
            addToValues(type.ordinal)
        }

        states?.let {
            addToWhere(JdbcUtils.inClause("job.int_state", it.size))
            addToValues(it.map { s -> s.ordinal })
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("job.str_name", it.size))
            addToValues(it)
        }

        paused?.let {
            addToWhere("job.bool_paused=?")
            addToValues(paused)
        }
    }
}
