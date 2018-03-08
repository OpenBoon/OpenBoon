package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Command
import com.zorroa.archivist.domain.CommandSpec
import com.zorroa.archivist.domain.CommandType
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.security.getUserId
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface CommandDao : GenericDao<Command, CommandSpec> {

    fun getPendingByUser(): List<Command>

    fun getNext(): Command?

    fun start(cmd: Command): Boolean

    fun stop(cmd: Command, msg: String?): Boolean

    fun cancel(cmd: Command, msg: String): Boolean

    fun updateProgress(cmd: Command, total: Long, success: Long, error: Long): Boolean
}

@Repository
class CommandDaoImpl : AbstractDao(), CommandDao {

    @Autowired private var userDaoCache: UserDaoCache? = null

    private val MAPPER = RowMapper<Command>  { rs, _ ->
        val c = Command()
        c.id = rs.getInt("pk_command")
        c.type = CommandType.values()[rs.getInt("int_type")]
        c.args = Json.deserialize<List<Any>>(rs.getString("json_args"),
                object : TypeReference<List<Any>>() {

                })
        c.state = JobState.values()[rs.getInt("int_state")]
        c.user = userDaoCache!!.getUser(rs.getInt("pk_user"))
        c.totalCount = rs.getLong("int_total_count")
        c.successCount = rs.getLong("int_success_count")
        c.errorCount = rs.getLong("int_error_count")
        c.message = rs.getString("str_message")

        val startTime = rs.getLong("time_started")
        if (startTime > 0) {
            var stopTime = rs.getLong("time_stopped")
            if (stopTime <= 0) {
                stopTime = System.currentTimeMillis()
            }
            c.duration = Math.max(0, stopTime - startTime)
        }
        c
    }

    override fun create(spec: CommandSpec): Command {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_command"))
            ps.setInt(1, getUserId())
            ps.setLong(2, System.currentTimeMillis())
            ps.setInt(3, spec.type.ordinal)
            ps.setInt(4, JobState.Waiting.ordinal)
            ps.setString(5, Json.serializeToString(spec.args, "[]"))
            ps
        }, keyHolder)

        val id = keyHolder.key.toInt()
        return get(id)
    }

    override fun get(id: Int): Command {
        return jdbc.queryForObject<Command>(GET + "WHERE pk_command=?", MAPPER, id)
    }

    override fun getPendingByUser(): List<Command> {
        return jdbc.query<Command>(GET + "WHERE pk_user=? AND int_state IN (?,?) ORDER BY int_state, pk_command", MAPPER,
                getUserId(), JobState.Active.ordinal, JobState.Waiting.ordinal)
    }

    override fun refresh(obj: Command): Command {
        return get(obj.id)
    }

    override fun getAll(): List<Command> {
        return mutableListOf()
    }

    override fun getAll(paging: Pager): PagedList<Command> {
        return PagedList()
    }

    override fun update(id: Int, spec: Command): Boolean {
        return false
    }

    override fun delete(id: Int): Boolean {
        return false
    }

    override fun count(): Long {
        return 0
    }

    override fun getNext(): Command? {
        return try {
            jdbc.queryForObject<Command>(
                    GET + "WHERE int_state=? ORDER BY time_created ASC LIMIT 1",
                    MAPPER, JobState.Waiting.ordinal)
        } catch (e: EmptyResultDataAccessException) {
            // just return null;
            null
        }

    }

    override fun start(cmd: Command): Boolean {
        return jdbc.update("UPDATE command SET time_started=?, int_state=? WHERE pk_command=? AND int_state=?",
                System.currentTimeMillis(), JobState.Active.ordinal, cmd.id, JobState.Waiting.ordinal) > 0
    }

    override fun stop(cmd: Command, msg: String?): Boolean {
        return jdbc.update("UPDATE command SET time_stopped=?, int_state=?, str_message=? WHERE pk_command=? AND int_state=?",
                System.currentTimeMillis(), JobState.Finished.ordinal, msg, cmd.id, JobState.Active.ordinal) > 0
    }

    override fun cancel(cmd: Command, msg: String): Boolean {
        return jdbc.update("UPDATE command SET time_stopped=?, int_state=?, str_message=? WHERE pk_command=?",
                System.currentTimeMillis(), JobState.Cancelled.ordinal, msg, cmd.id) > 0
    }

    override fun updateProgress(cmd: Command, total: Long, success: Long, error: Long): Boolean {
        return jdbc.update(UPDATE_PROGRESS, total, success, error, cmd.id) == 1
    }

    companion object {

        private val INSERT = JdbcUtils.insert("command",
                "pk_user",
                "time_created",
                "int_type",
                "int_state",
                "json_args")

        private val GET = "SELECT * FROM command "

        private val UPDATE_PROGRESS = "UPDATE " +
                "command " +
                "SET " +
                    "int_total_count=?," +
                    "int_success_count=int_success_count+?," +
                    "int_error_count=int_error_count+? " +
                "WHERE " +
                    "pk_command=?"
    }
}
