package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.DaoFilter
import com.zorroa.archivist.rest.UserController
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import java.util.*

enum class TaskState {
    Waiting,
    Running,
    Success,
    Failure,
    Skipped,
    Queued;

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

open class Task (
        val id: UUID,
        override val jobId: UUID,
        val organizationId: UUID,
        val name: String,
        val state: TaskState,
        val host: String?
) : TaskId, JobId {
    override val taskId = id

    /**
     * Return FileStorageSpec for where this tasks log file should go.
     */
    @JsonIgnore
    fun getLogSpec() : FileStorageSpec {
        return FileStorageSpec("job", jobId, "logs/$taskId.log")
    }

    override fun toString(): String {
        return "<Task id='$id' name='$name'/>"
    }

}

class DispatchTask(
        id: UUID,
        jobId: UUID,
        organizationId: UUID,
        name: String,
        state: TaskState,
        host: String?,
        val script: ZpsScript,
        var env: MutableMap<String,String>,
        var args: MutableMap<String,Object>,
        val userId: UUID,
        var logFile: String?=null) : Task(id, jobId, organizationId, name, state, host), TaskId {

    override val taskId = id
}

class TaskFilter (
        val ids : List<UUID>? = null,
        val states : List<TaskState>? = null,
        val jobIds: List<UUID>? = null
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
    }
}