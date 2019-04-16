package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.UserBase
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class JobState {
    Active,
    Cancelled,
    Finished,
    Archived
}


interface JobId {
    val jobId: UUID
}


class JobSpec (
        var name: String?,
        var script : ZpsScript?,
        val args: MutableMap<String, Any>? = mutableMapOf(),
        val env: MutableMap<String, String>? =  mutableMapOf(),
        val priority: Int=100,
        var paused: Boolean=false,
        val pauseDurationSeconds: Long?=null,
        val replace:Boolean=false
)

class JobUpdateSpec (
        var name: String,
        val priority: Int,
        val paused: Boolean,
        val timePauseExpired: Long
)

class Job (
        val id: UUID,
        val organizationId: UUID,
        val name: String,
        val type: PipelineType,
        val state: JobState,
        var assetCounts: Map<String,Int>?=null,
        var taskCounts: Map<String,Int>?=null,
        var createdUser: UserBase?=null,
        var timeStarted: Long,
        var timeUpdated: Long,
        var timeCreated: Long,
        var priority: Int,
        val paused: Boolean,
        val timePauseExpired: Long
) : JobId {
    override val jobId = id

    @JsonIgnore
    fun getStorageId() : String {
        return "job___${jobId}"
    }
}

class JobFilter (
        val ids : List<UUID>? = null,
        val type: PipelineType? = null,
        val states : List<JobState>? = null,
        val organizationIds: List<UUID>? = null,
        val names: List<String>? = null,
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
            addToValues(it.map{ s-> s.ordinal})
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("job.str_name", it.size))
            addToValues(it)
        }

        paused?.let {
            addToWhere("job.bool_paused=?")
            addToValues(paused)
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
    }
}

