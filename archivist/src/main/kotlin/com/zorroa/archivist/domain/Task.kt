package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.DaoFilter
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class TaskState {
    Waiting,
    Running,
    Success,
    Failure,
    Skipped,
    Queued;

    fun isDispatched() : Boolean {
        return this == TaskState.Running || this == TaskState.Queued
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
        return FileStorageSpec("log", id.toString(), "log", jobId = jobId, taskId = id)
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
    override val sortMap: Map<String, String> = mapOf()

    @JsonIgnore
    override fun build() {

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