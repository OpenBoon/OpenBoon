package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.common.repository.DaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class JobState {
    WAITING,
    RUNNING,
    SUCCESS,
    FAIL,
    ORPHAN
}

data class JobSpec (
        val name: String,
        val assetId: UUID,
        val organizationId: UUID,
        val pipelines: List<String>,
        val attrs: Map<String, Any>? = null,
        val env: Map<String, String>? = null
)

data class Job (
        val id: UUID,
        val assetId: UUID,
        val organizationId: UUID,
        val name: String,
        val state: JobState,
        val pipelines: List<String>,
        val attrs: Map<String, Any> = mutableMapOf(),
        val env: Map<String, String> = mutableMapOf()
)

data class JobFilter (
        private val ids : List<UUID>? = null,
        private val assetIds : List<UUID>? = null,
        private val states : List<JobState>? = null,
        private val organizationIds: List<UUID>? = null
) : DaoFilter() {

    override val sortMap: Map<String, String>? = null

    @JsonIgnore
    override fun build() {

        if (!ids.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("job.pk_job", ids!!.size))
            addToValues(ids)
        }

        if (!assetIds.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("job.pk_asset", assetIds!!.size))
            addToValues(assetIds)
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
