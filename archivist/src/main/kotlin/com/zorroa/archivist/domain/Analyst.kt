package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.common.repository.DaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class AnalystState {
    Up,
    Down
}

enum class LockState {
    Unlocked,
    Locked
}

class AnalystSpec (
        val endpoint: String,
        val taskId: UUID?,
        val totalRamMb: Int,
        val freeRamMb: Int,
        val load: Float
)

class Analyst (
        val id: UUID,
        val taskId: UUID?,
        val endpoint: String,
        val totalRamMb: Int,
        val freeRamMb: Int,
        val load: Float,
        val timePing: Long,
        val timeCreated: Long,
        val state: AnalystState,
        val lock: LockState
)

data class AnalystFilter (
        private val ids : List<UUID>? = null,
        private val states : List<AnalystState>? = null,
        private val taskIds: List<UUID>? = null
) : DaoFilter() {

    override val sortMap: Map<String, String>? = null

    @JsonIgnore
    override fun build() {

        if (!ids.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("analyst.pk_analyst", ids!!.size))
            addToValues(ids)
        }

        if (!states.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("analyst.int_state", states!!.size))
            addToValues(states.map{it.ordinal})
        }

        if (!taskIds.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("analyst.pk_task", taskIds!!.size))
            addToValues(taskIds)
        }
    }
}