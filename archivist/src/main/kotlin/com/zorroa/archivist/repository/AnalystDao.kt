package com.zorroa.archivist.repository

import com.zorroa.common.domain.*
import com.zorroa.common.util.JdbcUtils.insert
import com.zorroa.common.util.JdbcUtils.update
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface AnalystDao {
    fun create(spec: AnalystSpec) : Analyst
    fun update(spec: AnalystSpec) : Boolean
    fun get(id: UUID): Analyst
    fun get(endpoint: String): Analyst
    fun exists(endpoint: String): Boolean
    fun setState(analyst: Analyst, state: AnalystState): Boolean
}

@Repository
class AnalystDaoImpl : AbstractDao(), AnalystDao {

    override fun create(spec: AnalystSpec) : Analyst {
        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        jdbc.update(INSERT, id, spec.taskId, time, time, spec.endpoint,
                spec.totalRamMb, spec.freeRamMb, spec.load)
        return get(id)
    }

    override fun update(spec: AnalystSpec) : Boolean {
        val time = System.currentTimeMillis()
        return jdbc.update(UPDATE, spec.taskId, time, spec.totalRamMb, spec.freeRamMb, spec.load, spec.endpoint) == 1
    }

    override fun get(id: UUID): Analyst {
        return jdbc.queryForObject("$GET WHERE pk_analyst=?", MAPPER, id)
    }

    override fun get(endpoint: String): Analyst {
        return jdbc.queryForObject("$GET WHERE str_endpoint=?", MAPPER, endpoint)
    }

    override fun exists(endpoint: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM analyst WHERE str_endpoint=?", Int::class.java, endpoint) == 1
    }

    override fun setState(analyst: Analyst, state: AnalystState) : Boolean {
        return jdbc.update("UPDATE analyst SET int_state=? WHERE pk_analyst=? AND int_state != ?",
                state.ordinal, analyst.id, state.ordinal) == 1
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Analyst(
                    rs.getObject("pk_analyst") as UUID,
                    rs.getObject("pk_task") as UUID?,
                    rs.getString("str_endpoint"),
                    rs.getInt("int_total_ram"),
                    rs.getInt("int_free_ram"),
                    rs.getFloat("flt_load"),
                    rs.getLong("time_ping"),
                    rs.getLong("time_created"),
                    AnalystState.values()[rs.getInt("int_state")],
                    LockState.values()[rs.getInt("int_lock_state")])
        }

        private const val GET = "SELECT * FROM analyst"

        private const val COUNT = "SELECT COUNT(1) FROM analyst"

        private val INSERT = insert("analyst",
                "pk_analyst",
                "pk_task",
                "time_created",
                "time_ping",
                "str_endpoint",
                "int_total_ram",
                "int_free_ram",
                "flt_load")

        private val UPDATE = update("analyst",
                "str_endpoint",
                "pk_task",
                "time_ping",
                "int_total_ram",
                "int_free_ram",
                "flt_load")
    }

}