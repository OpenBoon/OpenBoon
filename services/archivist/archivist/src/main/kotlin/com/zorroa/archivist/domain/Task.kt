package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.TaskState.Failure
import com.zorroa.archivist.domain.TaskState.Queued
import com.zorroa.archivist.domain.TaskState.Running
import com.zorroa.archivist.domain.TaskState.Skipped
import com.zorroa.archivist.domain.TaskState.Success
import com.zorroa.archivist.domain.TaskState.Waiting
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.JdbcUtils
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
    Queued;

    /**
     * Return true of the TaskState is Running or Queued
     */
    fun isDispatched(): Boolean {
        return this == Running || this == Queued
    }

    /**
     * Return a Micrometer tag for tagging metrics related to this state.
     */
    fun metricsTag(): Tag {
        return Tag.of("task-state", this.toString())
    }
}

class TaskSpec(
    val name: String,
    val script: ZpsScript
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
@ApiModel("InternalTask", description = "An InternalTask implementation has enough properties to accomplish any internal operations but isn't intended for client use.")
open class InternalTask(
    override val taskId: UUID,
    override val jobId: UUID,

    @ApiModelProperty("UUID of the Project this Task belongs to.")
    val projectId: UUID,
    @ApiModelProperty("name The [Task] name")
    val name: String,
    @ApiModelProperty("The current [TaskState] of the [Task]")
    val state: TaskState

) : TaskId, JobId {
    override fun toString(): String {
        return "<Task id='$taskId' name='$name'/>"
    }

    /**
     * Return FileStorageSpec for where this tasks log file should go.
     */
    @JsonIgnore
    fun getLogSpec(): FileStorageSpec {
        return FileStorageSpec("job", jobId, "logs/$taskId.log")
    }
}

@ApiModel("Task", description = "Describes a Task.")
open class Task(

    @ApiModelProperty("UUID of the Task.")
    val id: UUID,

    @ApiModelProperty("UUID of the Job this Task belongs to.")
    override val jobId: UUID,

    projectId: UUID,

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
    val assetCounts: Map<String, Int>

) : InternalTask(id, jobId, projectId, name, state)

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
    name: String,
    state: TaskState,
    val host: String?,
    val script: ZpsScript,
    var env: MutableMap<String, String>,
    var args: MutableMap<String, Any>,
    var logFile: String? = null
) : InternalTask(id, jobId, projectId, name, state), TaskId {

    override val taskId = id
}

@ApiModel("Task Filter", description = "Search filter for finding Tasks.")
class TaskFilter(

    @ApiModelProperty("Task UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("States to match.")
    val states: List<TaskState>? = null,

    @ApiModelProperty("Job UUIDs to match.")
    val jobIds: List<UUID>? = null,

    @ApiModelProperty("Task names to match.")
    val names: List<String>? = null,

    @ApiModelProperty("Project UUIDs to match.")
    val projectIds: List<UUID>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("taskId" to "task.pk_task",
                    "id" to "task.pk_task",
                    "jobId" to "task.pk_job",
                    "state" to "task.int_state",
                    "timeCreated" to "task.time_created",
                    "timeStarted" to "task.time_started")

    @JsonIgnore
    override fun build() {

        if (sort == null) {
            sort = listOf("taskId:a")
        }

        addToWhere("job.project_id=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("task.pk_task", it.size))
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