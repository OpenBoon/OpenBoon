package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.domain.JobPriority.Interactive
import boonai.archivist.domain.JobPriority.Reindex
import boonai.archivist.domain.JobPriority.Standard
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class JobState {
    /**
     * The job has [TaskState.Running] or [TaskState.Queued] tasks.
     */
    InProgress,

    /**
     * The job was manually canceled by the user.
     */
    Cancelled,

    /**
     * All tasks in the job are [TaskState.Success] or [TaskState.Skipped]
     */
    Success,

    /**
     * All task data, temp files, and logs for the job have been removed.
     */
    Archived,

    /**
     * All tasks in the job are [TaskState.Failure], [TaskState.Success] or [TaskState.Skipped]
     */
    Failure;

    /**
     * Return true if transitioning to the current state puts the job
     * in an inactive state.
     */
    fun isInactiveState(): Boolean {
        return this == Success || this == Failure || this == Cancelled
    }

    /**
     * Return true if transitioning to the current state puts the job
     * in an active state.
     */
    fun isActiveState(): Boolean {
        return this == InProgress
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
    var scripts: List<ZpsScript>?,

    /**
     * Cannot be specified from external source, can only be set
     * by the DataSourceService.
     */
    @JsonIgnore
    @ApiModelProperty("Unique ID of the DataSource, if any.", hidden = true)
    var dataSourceId: UUID? = null,

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
    val maxRunningTasks: Int = 1024,

    @ApiModelProperty("The name of the credentials blobs available to this job.")
    val credentials: Set<String>? = null,

    @ApiModelProperty("A list of job IDs to depend on.")
    var dependOnJobIds: List<UUID>? = null
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
    val projectId: UUID,

    @ApiModelProperty("Unique ID of the DataSource, if any.")
    var dataSourceId: UUID?,

    @ApiModelProperty("Name of the Job.")
    val name: String,

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

    @ApiModelProperty("Time the job went into the inactive state.")
    var timeStopped: Long,

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

    @ApiModelProperty("States to match.")
    val states: List<JobState>? = null,

    @ApiModelProperty("Job names to match.")
    val names: List<String>? = null,

    @ApiModelProperty("Paused status to match.")
    val paused: Boolean? = null,

    @ApiModelProperty("The DataSource Ids to match")
    val datasourceIds: List<UUID>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "job.pk_job",
            "name" to "job.str_name",
            "timeCreated" to "job.time_created",
            "state" to "job.int_state",
            "priority" to "job.int_priority",
            "projectId" to "job.pk_project",
            "dataSourceId" to "job.pk_datasource",
            "runningTasks" to "job_count.int_task_state_1",
            "waitingTasks" to "job_count.int_task_state_0"
        )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("runningTasks:desc", "waitingTasks:desc", "priority:asc", "timeCreated:asc")
        }

        addToWhere("job.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("job.pk_job", it.size))
            addToValues(it)
        }

        datasourceIds?.let {
            addToWhere(JdbcUtils.inClause("job.pk_datasource", it.size))
            addToValues(it)
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

/**
 * A simple class for determining the dispatch priority of project.
 *
 * @property projectId The Organization Id.
 * @property priority The priority of the Organization, lower is higher priority.
 */
class DispatchPriority(
    val projectId: UUID,
    val priority: Int
)

/**
 * Provides task state counts for a particular jobs.
 */
class TaskStateCounts(
    val counts: Map<TaskState, Int>,
    val total: Int
) {
    fun hasPendingTasks(): Boolean {
        return (
            counts.getValue(TaskState.Waiting) > 0 ||
                counts.getValue(TaskState.Running) > 0 ||
                counts.getValue(TaskState.Queued) > 0
            )
    }

    fun hasFailures(): Boolean {
        return counts.getValue(TaskState.Failure) > 0
    }
}
