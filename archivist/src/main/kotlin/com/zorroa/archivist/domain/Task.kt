package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.repository.DaoFilter
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class TaskState {
    Waiting,
    Running,
    Success,
    Fail,
    Skip,
    Queue
}

class TaskSpec(
        val name: String,
        val script: ZpsScript
)

interface TaskId {
    val taskId: UUID
}

open class Task(
        val id: UUID,
        val jobId: UUID,
        val organizationId: UUID,
        val name: String,
        val state: TaskState
) : TaskId {
    override val taskId = id
}

class DispatchTask(
        id: UUID,
        jobId: UUID,
        organizationId: UUID,
        name: String,
        state: TaskState,
        val script: ZpsScript,
        var env: MutableMap<String,String>,
        var args: MutableMap<String,Object>) : Task(id, jobId, organizationId, name, state), TaskId {

    override val taskId = id
}

class TaskError(
        val id: UUID,
        val taskId: UUID,
        val jobId: UUID,
        val assetId: UUID,
        val path: String,
        val message: String,
        val processor: String,
        val fatal: Boolean,
        val endpoint: String
)

class TaskErrorEvent(
        val assetId: UUID,
        val path: String,
        val message: String,
        val processor: String,
        val fatal: Boolean
)

class Expand(
        val endpoint: String,
        val jobId: UUID,
        val taskId: UUID,
        val script: ZpsScript)


data class TaskFilter (
        private val ids : List<UUID>? = null,
        private val states : List<JobState>? = null,
        private val jobIds: List<UUID>? = null
) : KDaoFilter() {

    override val sortMap: Map<String, String>? = null

    @JsonIgnore
    override fun build() {

        if (!ids.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("task.pk_task", ids!!.size))
            addToValues(ids)
        }

        if (!states.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("task.int_state", states!!.size))
            addToValues(states.map{it.ordinal})
        }

        if (!jobIds.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("task.pk_job", jobIds!!.size))
            addToValues(jobIds)
        }
    }
}