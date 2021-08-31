package boonai.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import boonai.archivist.domain.TaskState.Failure
import boonai.archivist.domain.TaskState.Queued
import boonai.archivist.domain.TaskState.Running
import boonai.archivist.domain.TaskState.Skipped
import boonai.archivist.domain.TaskState.Success
import boonai.archivist.domain.TaskState.Waiting
import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import io.micrometer.core.instrument.Tag
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * The state of a Task.
 * @property Waiting The [Task] is waiting to be dispatched.
 * @property Running The [Task] is running on an [Analyst].
 * @property Success The [Task] has completed successfully.
 * @property Failure The [Task] has failed with a non-zero exit status.
 * @property Skipped The [Task] was manually skipped.
 * @property Queued The [Task] was queued by an [Analyst].
 */
enum class TaskState {
    Waiting,
    Running,
    Success,
    Failure,
    Skipped,
    Queued,
    Depend;

    /**
     * Return true of the TaskState is Running or Queued
     */
    fun isDispatched(): Boolean {
        return this == Running || this == Queued
    }

    fun isWaitingState(): Boolean {
        return this == Waiting
    }

    /**
     * Return a Micrometer tag for tagging metrics related to this state.
     */
    fun metricsTag(): Tag {
        return Tag.of("task-state", this.toString())
    }

    fun isFinishedState(): Boolean {
        return this == Success || this == Failure || this == Skipped
    }

    fun isSuccessState(): Boolean {
        return this == Success || this == Skipped
    }
}

class TaskSpec(
    val name: String,
    val script: ZpsScript
)

open class PendingTasksStats(
    val pendingTasks: Long = 0L,
    val runningTasks: Long = 0L,
    val maxRunningTasks: Long = 0L
)

interface TaskId {
    val taskId: UUID
}

/**
 * An InternalTask implementation has enough properties to accomplish any internal
 * operations but isn't intended for client use.
 *
 * @property taskId The unique ID of the [Task].
 * @property jobId The unique ID pf the [Job].
 * @property name The [Task] name
 * @property state The current [TaskState] of the [Task]
 */
@ApiModel(
    "InternalTask",
    description = "An InternalTask implementation has enough properties to accomplish any internal operations but isn't intended for client use."
)
open class InternalTask(
    @ApiModelProperty("The unique ID of the Task.")
    override val taskId: UUID,
    @ApiModelProperty("The unique ID pf the Job")
    override val jobId: UUID,
    @ApiModelProperty("UUID of the Project this Task belongs to.")
    val projectId: UUID,
    @ApiModelProperty("The DataSource this task is processing.")
    val dataSourceId: UUID?,
    @ApiModelProperty("name The [Task] name")
    val name: String,
    @ApiModelProperty("The current [TaskState] of the [Task]")
    val state: TaskState

) : TaskId, JobId {
    override fun toString(): String {
        return "<Task id='$taskId' name='$name'/>"
    }

    @JsonIgnore
    fun getLogFileLocation(): ProjectFileLocator {
        return ProjectFileLocator(ProjectStorageEntity.JOB, jobId.toString(), "logs", "$taskId.log")
    }
}

@ApiModel("Task", description = "Describes a Task.")
open class Task(

    @ApiModelProperty("UUID of the Task.")
    val id: UUID,

    @ApiModelProperty("UUID of the Job this Task belongs to.")
    override val jobId: UUID,

    projectId: UUID,

    dataSourceId: UUID?,

    name: String,

    state: TaskState,

    @ApiModelProperty("Host endpoint of the last Analyst the Task ran on.")
    val host: String?,

    @ApiModelProperty("Time the Task was started.")
    val timeStarted: Long,

    @ApiModelProperty("Time the Task was stopped.")
    val timeStopped: Long,

    @ApiModelProperty("Time the Task was created.")
    val timeCreated: Long,

    @ApiModelProperty("Time the Task last received a pint from an Analyst.")
    val timePing: Long,

    @ApiModelProperty("Counters for the total number of assets created, updated, etc.")
    val assetCounts: Map<String, Int>,

    @ApiModelProperty("Task Progress")
    var progress: Int = 0,

    @ApiModelProperty("Task run count")
    var runCount: Int,

    @ApiModelProperty("Current Task Status")
    var status: String? = null

) : InternalTask(id, jobId, projectId, dataSourceId, name, state) {

    val logName = "$id-$runCount"
}

/**
 * A DispatchTask is used by the Analysts to start a new task.
 *
 * @property projectId The Project the task belongs to.
 * @property script The [ZpsScript] to run.
 * @property env Extra ENV variables to apply before starting task.
 * @property args Extra script args to pass to the [ZpsScript]
 * @property userId The UUID of the user running the [Task]
 * @property logFile The path to the [Task] log file.
 *
 */
class DispatchTask(
    val id: UUID,
    jobId: UUID,
    projectId: UUID,
    dataSourceId: UUID?,
    name: String,
    state: TaskState,
    val host: String?,
    @ApiModelProperty("The ZpsScript to run.")
    val script: ZpsScript,
    @ApiModelProperty("Extra ENV variables to apply before starting task.")
    var env: MutableMap<String, String>,
    @ApiModelProperty("Extra script args to pass to the ZpsScript")
    var args: MutableMap<String, Any>,
    @ApiModelProperty("The Id of the log file.")
    var logName: String,
    @ApiModelProperty("The path to the Task log file.")
    var logFile: String? = null
) : InternalTask(id, jobId, projectId, dataSourceId, name, state), TaskId {

    override val taskId = id
}

@ApiModel("Task Filter", description = "Search filter for finding Tasks.")
class TaskFilter(

    @ApiModelProperty("Task UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("DataSource UUIDs to match.")
    val dataSourceIds: List<UUID>? = null,

    @ApiModelProperty("States to match.")
    val states: List<TaskState>? = null,

    @ApiModelProperty("Job UUIDs to match.")
    val jobIds: List<UUID>? = null,

    @ApiModelProperty("Task names to match.")
    val names: List<String>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "taskId" to "task.pk_task",
            "id" to "task.pk_task",
            "jobId" to "task.pk_job",
            "state" to "task.int_state",
            "timeCreated" to "task.time_created",
            "timeStarted" to "task.time_started"
        )

    private val defaultSort = "case " +
        "when task.int_state = 1 then (1, task.time_created) " +
        "when task.int_state = 3 then (2, task.time_created) " +
        "when task.int_state = 0 then (3, task.time_created) " +
        "else (4, task.time_created) " +
        "end "

    @JsonIgnore
    override fun build() {

        if (sort == null) {
            sortRaw = listOf(defaultSort)
        }

        addToWhere("job.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("task.pk_task", it.size))
            addToValues(it)
        }

        dataSourceIds?.let {
            addToWhere(JdbcUtils.inClause("job.pk_datasource", it.size))
            addToValues(it)
        }

        states?.let {
            addToWhere(JdbcUtils.inClause("task.int_state", it.size))
            addToValues(it.map { s -> s.ordinal })
        }

        jobIds?.let {
            addToWhere(JdbcUtils.inClause("task.pk_job", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("task.str_name", it.size))
            addToValues(it)
        }
    }
}
