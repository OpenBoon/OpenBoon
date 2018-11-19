package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class AnalystState {
    Down,
    Up;

    @JsonValue
    fun toValue() : Int {
        return ordinal
    }
}

enum class LockState {
    Unlocked,
    Locked;

    @JsonValue
    fun toValue() : Int {
        return ordinal
    }
}

class AnalystSpec (
        val totalRamMb: Int,
        val freeRamMb: Int,
        val load: Float,
        val taskId: UUID?=null)


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
) : KDaoFilter() {

    override val sortMap: Map<String, String> = mapOf()

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