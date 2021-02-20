package boonai.archivist.repository

import boonai.archivist.domain.Analyst
import boonai.archivist.domain.AnalystFilter
import boonai.archivist.domain.AnalystSpec
import boonai.archivist.domain.AnalystState
import boonai.archivist.domain.LockState
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.archivist.security.getAnalyst
import boonai.archivist.util.JdbcUtils.insert
import boonai.archivist.util.JdbcUtils.update
import boonai.common.service.logging.event
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.net.URI
import java.time.Duration
import java.util.UUID

interface AnalystDao {
    fun create(spec: AnalystSpec): Analyst
    fun update(spec: AnalystSpec): Boolean
    fun get(id: UUID): Analyst
    fun get(endpoint: String): Analyst
    fun exists(endpoint: String): Boolean
    fun setState(analyst: Analyst, state: AnalystState): Boolean
    fun getAll(filter: AnalystFilter): KPagedList<Analyst>
    fun count(filter: AnalystFilter): Long
    fun setLockState(analyst: Analyst, state: LockState): Boolean
    fun isInLockState(endpoint: String, state: LockState): Boolean
    fun setTaskId(endpoint: String, taskId: UUID?): Boolean
    fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst>
    fun delete(analyst: Analyst): Boolean
    fun findOne(filter: AnalystFilter): Analyst
}

@Repository
class AnalystDaoImpl : AbstractDao(), AnalystDao {

    override fun create(spec: AnalystSpec): Analyst {
        val id = uuid1.generate()
        val endpoint = spec.endpoint ?: getAnalyst().endpoint
        if (!URI(endpoint).scheme.startsWith("http")) {
            throw IllegalArgumentException("The analyst endpoint must be an http URL.")
        }
        val time = System.currentTimeMillis()
        jdbc.update(
            INSERT, id, spec.taskId, time, time, endpoint,
            spec.totalRamMb, spec.freeRamMb, spec.freeDiskMb, spec.load,
            AnalystState.Up.ordinal, spec.version
        )
        return get(id)
    }

    override fun update(spec: AnalystSpec): Boolean {
        val time = System.currentTimeMillis()
        val endpoint = getAnalyst().endpoint
        return jdbc.update(
            UPDATE, spec.taskId, time, spec.totalRamMb,
            spec.freeRamMb, spec.freeDiskMb, spec.load, AnalystState.Up.ordinal,
            spec.version, endpoint
        ) == 1
    }

    override fun findOne(filter: AnalystFilter): Analyst {
        val query = filter.getQuery(GET)
        val values = filter.getValues()
        return jdbc.queryForObject(query, MAPPER, *values)
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

    override fun setState(analyst: Analyst, state: AnalystState): Boolean {
        val result = jdbc.update(
            "UPDATE analyst SET int_state=? WHERE pk_analyst=? AND int_state != ?",
            state.ordinal, analyst.id, state.ordinal
        ) == 1
        if (result) {
            logger.event(
                LogObject.ANALYST, LogAction.STATE_CHANGE,
                mapOf("newState" to state, "oldState" to analyst.state)
            )
        }
        return result
    }

    override fun setLockState(analyst: Analyst, state: LockState): Boolean {
        return jdbc.update(
            "UPDATE analyst SET int_lock_state=? WHERE pk_analyst=? AND int_lock_state != ?",
            state.ordinal, analyst.id, state.ordinal
        ) == 1
    }

    override fun isInLockState(endpoint: String, state: LockState): Boolean {
        return jdbc.queryForObject(
            "SELECT COUNT(1) FROM analyst WHERE str_endpoint=? AND int_lock_state=?",
            Int::class.java, endpoint, state.ordinal
        ) == 1
    }

    override fun setTaskId(endpoint: String, taskId: UUID?): Boolean {
        return jdbc.update("UPDATE analyst SET pk_task=? WHERE str_endpoint=?", taskId, endpoint) == 1
    }

    override fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst> {
        val time = System.currentTimeMillis() - duration.toMillis()
        return jdbc.query(GET_DOWN, MAPPER, state.ordinal, time)
    }

    override fun delete(analyst: Analyst): Boolean {
        val result = jdbc.update("DELETE FROM analyst WHERE pk_analyst=?", analyst.id) == 1
        if (result) {
            logger.event(LogObject.ANALYST, LogAction.DELETE)
        }
        return result
    }

    override fun getAll(filter: AnalystFilter): KPagedList<Analyst> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(filter: AnalystFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Analyst(
                rs.getObject("pk_analyst") as UUID,
                rs.getObject("pk_task") as UUID?,
                rs.getString("str_endpoint"),
                rs.getInt("int_total_ram"),
                rs.getInt("int_free_ram"),
                rs.getInt("int_free_disk"),
                rs.getFloat("flt_load"),
                rs.getLong("time_ping"),
                rs.getLong("time_created"),
                AnalystState.values()[rs.getInt("int_state")],
                LockState.values()[rs.getInt("int_lock_state")],
                rs.getString("str_version")
            )
        }

        private const val GET = "SELECT * FROM analyst"

        private const val GET_DOWN = "SELECT * FROM analyst " +
            "WHERE int_state=? AND time_ping < ?"

        private const val COUNT = "SELECT COUNT(1) FROM analyst"

        private val INSERT = insert(
            "analyst",
            "pk_analyst",
            "pk_task",
            "time_created",
            "time_ping",
            "str_endpoint",
            "int_total_ram",
            "int_free_ram",
            "int_free_disk",
            "flt_load",
            "int_state",
            "str_version"
        )

        private val UPDATE = update(
            "analyst",
            "str_endpoint",
            "pk_task",
            "time_ping",
            "int_total_ram",
            "int_free_ram",
            "int_free_disk",
            "flt_load",
            "int_state",
            "str_version"
        )
    }
}
