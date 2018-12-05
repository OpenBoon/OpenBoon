package com.zorroa.common.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonValue
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class AnalystState {
    Down,
    Up
}

enum class LockState {
    Unlocked,
    Locked
}

class AnalystSpec (
        val totalRamMb: Int,
        val freeRamMb: Int,
        val freeDiskMb: Int,
        val load: Float,
        val taskId: UUID?=null)
{
    @JsonIgnore
    var endpoint: String? = null
}


class Analyst (
        val id: UUID,
        val taskId: UUID?,
        val endpoint: String,
        val totalRamMb: Int,
        val freeRamMb: Int,
        val freeDiskMb: Int,
        val load: Float,
        val timePing: Long,
        val timeCreated: Long,
        val state: AnalystState,
        val lock: LockState
) {
    override fun toString() : String {
        return "<Analyst id='$id' endpoint='$endpoint'}"
    }
}

data class AnalystFilter (
        val ids : List<UUID>? = null,
        val states : List<AnalystState>? = null,
        val taskIds: List<UUID>? = null,
        val lockStates: List<LockState>? = null,
        val endpoints: List<String>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
            "id" to "analyst.pk_analyst",
            "load" to "analyst.flt_load",
            "endpoint" to "analyst.str_endpoint",
            "lock" to "analyst.int_lock_state",
            "timePing" to "analyst.time_ping")

    @JsonIgnore
    override fun build() {

        ids?.let {
            addToWhere(JdbcUtils.inClause("analyst.pk_analyst", it.size))
            addToValues(it)
        }

        endpoints?.let {
            addToWhere(JdbcUtils.inClause("analyst.str_endpoint", it.size))
            addToValues(it)
        }

        states?.let {
            addToWhere(JdbcUtils.inClause("analyst.int_state", it.size))
            addToValues(it.map{s-> s.ordinal})
        }

        lockStates?.let {
            addToWhere(JdbcUtils.inClause("analyst.int_lock_state", it.size))
            addToValues(it.map{s-> s.ordinal})
        }

        taskIds?.let {
            addToWhere(JdbcUtils.inClause("analyst.pk_task", it.size))
            addToValues(it)
        }
    }
}