package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import io.micrometer.core.instrument.Tag
import java.util.*

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
    fun isDispatched() : Boolean {
        return this == Running || this == Queued
    }

    /**
     * Return a Micrometer tag for tagging metrics related to this state.
     */
    fun metricsTag() : Tag {
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
open class InternalTask(
    override val taskId: UUID,
    override val jobId: UUID,
    val name: String,
    val state: TaskState
) : TaskId, JobId
{
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

/**
 * The standad Task implementation return to the clients.
 *
 * @property id The unique ID of the [Task].
 * @property jobId The unique ID of the [Job].
 * @property organizationId The unique ID of the Organization.
 * @property name The [Task] name
 * @property state The current [TaskState] of the [Task]
 * @property host The host endpoint of the last Analyst the task ran on.
 * @property timeStarted The time the [Task] was started.
 * @property timeStopped The time the [Task] was stopped.
 * @property timeCreated The time the [Task] was created.
 * @property timePing The time the [Task] got a ping from an [Analyst]
 * @property assetCounts Counters for the total number of assets created, updated, etc.
 */
open class Task (
        val id: UUID,
        override val jobId: UUID,
        val organizationId: UUID,
        name: String,
        state: TaskState,
        val host: String?,
        val timeStarted: Long,
        val timeStopped: Long,
        val timeCreated: Long,
        val timePing: Long,
        val assetCounts: Map<String,Int>
) : InternalTask (id, jobId, name, state)

/**
 * A DispatchTask is used by the Analysts to start a new task.
 *
 * @property organizationId The Organization the task belongs to.
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
        val organizationId: UUID,
        name: String,
        state: TaskState,
        val host: String?,
        val script: ZpsScript,
        var env: MutableMap<String,String>,
        var args: MutableMap<String,Object>,
        val userId: UUID,
        var logFile: String?=null) : InternalTask(id, jobId, name, state), TaskId {

    override val taskId = id
}

/**
 * A DAO filter for selecting tasks.
 *
 * @property ids An array of unique [Task] ids.
 * @property states An array of [TaskState]s.
 * @property jobIds An array of unique [Job] ids.
 * @property names An array of task names.
 */
class TaskFilter (
        val ids : List<UUID>? = null,
        val states : List<TaskState>? = null,
        val jobIds: List<UUID>? = null,
        val names: List<String>?=null,
        val organizationIds: List<UUID>? = null
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

        if (hasPermission("zorroa::superadmin")) {
            organizationIds?.let {
                addToWhere(JdbcUtils.inClause("job.pk_organization", it.size))
                addToValues(it)
            }
        }
        else {
            addToWhere("job.pk_organization=?")
            addToValues(getOrgId())
        }

        ids?.let  {
            addToWhere(JdbcUtils.inClause("task.pk_task", it.size))
            addToValues(it)
        }

        states?.let {
            addToWhere(JdbcUtils.inClause("task.int_state", it.size))
            addToValues(it.map{ s -> s.ordinal})
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