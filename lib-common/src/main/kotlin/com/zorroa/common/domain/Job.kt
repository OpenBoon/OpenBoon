package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.common.repository.DaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class JobState {
    SETUP,
    WAITING,
    RUNNING,
    SUCCESS,
    FAIL,
    ORPHAN,
    SKIP,
    QUEUE
}

data class JobSpec (
        val name: String,
        val type: PipelineType,
        val organizationId: UUID,
        val script: ZpsScript,
        val lockAssets : Boolean = false,
        val attrs: MutableMap<String, Any> = mutableMapOf(),
        val env: MutableMap<String, String> =  mutableMapOf()
)

data class Job (
        val id: UUID,
        val type: PipelineType,
        val organizationId: UUID,
        val name: String,
        val state: JobState,
        val lockAssets: Boolean,
        val attrs: Map<String, Any>,
        val env: Map<String, String>
)
{
    /**
     * The relative path to the job script, on any storage service.
     */
    fun getScriptPath() : String {
        return "zorroa/jobs/$id/script.zps"
    }
}

data class JobFilter (
        private val ids : List<UUID>? = null,
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
