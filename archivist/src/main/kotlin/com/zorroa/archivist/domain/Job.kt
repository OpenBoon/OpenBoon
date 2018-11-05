package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.UserBase
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.sql.ResultSet
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
        val env: MutableMap<String, String>? =  mutableMapOf()
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
        var timeUpdated: Long
) : JobId {
    override val jobId = id
}

class JobFilter (
        private val ids : List<UUID>? = null,
        private val type: PipelineType? = null,
        private val states : List<JobState>? = null,
        private val organizationIds: List<UUID>? = null
) : KDaoFilter() {

    override val sortMap: Map<String, String>? = null

    @JsonIgnore
    override fun build() {

        if (!ids.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("job.pk_job", ids!!.size))
            addToValues(ids)
        }

        if (type != null) {
            addToWhere("job.int_Type=?")
            addToValues(type.ordinal)
        }

        if (!states.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("job.int_state", states!!.size))
            addToValues(states.map{it.ordinal})
        }

        if (hasPermission("zorroa::superadmin")) {
            if (!organizationIds.orEmpty().isEmpty()) {
                addToWhere(JdbcUtils.inClause("job.pk_organization", organizationIds!!.size))
                addToValues(organizationIds)
            }
        }
        else {
            addToWhere("job.pk_organization=?")
            addToValues(getOrgId())
        }
    }
}

class JobEvent(val type:String, val payload: Any)

class JobStateChangeEvent(val job: Job, val newState: JobState, val oldState : JobState?)
