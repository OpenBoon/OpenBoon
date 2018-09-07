package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class JobState {
    Active,
    Cancelled,
    Finished
}

class JobSpec (
        val name: String,
        val type: PipelineType,
        val scripts : List<ZpsScript>,
        val args: MutableMap<String, Any> = mutableMapOf(),
        val env: MutableMap<String, String> =  mutableMapOf()
)

class Job (
        val id: UUID,
        val organizationId: UUID,
        val name: String,
        val type: PipelineType,
        val state: JobState
)

data class JobFilter (
        private val ids : List<UUID>? = null,
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

        if (!states.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("job.int_state", states!!.size))
            addToValues(states.map{it.ordinal})
        }

        if (!organizationIds.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("job.pk_organization", organizationIds!!.size))
            addToValues(organizationIds)
        }
    }
}

class JobEvent(val type:String, val payload: Any)

class JobStateChangeEvent(val job: Job, val newState: JobState, val oldState : JobState?)
