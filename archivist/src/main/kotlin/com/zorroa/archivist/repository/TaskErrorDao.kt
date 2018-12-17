package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getAnalystEndpoint
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface TaskErrorDao {
    fun create(event: TaskEvent, error: TaskErrorEvent): TaskError
    fun get(id: UUID) : TaskError
    fun getLast() : TaskError
    fun count(filter: TaskErrorFilter): Long
    fun getAll(filter: TaskErrorFilter) : KPagedList<TaskError>
    fun delete(id: UUID) : Boolean
    fun deleteAll(job: JobId) : Int
}

@Repository
class TaskErrorDaoImpl : AbstractDao(), TaskErrorDao {

    override fun create(event: TaskEvent, spec: TaskErrorEvent): TaskError {

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, event.taskId)
            ps.setObject(3, event.jobId)
            ps.setObject(4, spec.assetId)
            ps.setString(5, spec.message)
            ps.setString(6, spec.path)
            ps.setString(7, spec.processor)
            ps.setString(8, getAnalystEndpoint())
            ps.setString(9, FileUtils.extension(spec.path))
            ps.setBoolean(10, spec.fatal)
            ps.setString(11, spec.phase)
            ps.setLong(12, time)
            ps
        }

        return TaskError(
                id,
                event.taskId,
                event.jobId,
                spec.assetId,
                spec.path,
                spec.message,
                spec.processor,
                spec.fatal,
                getAnalystEndpoint(),
                spec.phase,
                time)
    }

    override fun count(filter: TaskErrorFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    override fun getAll(filter: TaskErrorFilter) : KPagedList<TaskError> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun get(id: UUID) : TaskError {
        return if (hasPermission("zorroa::superadmin")) {
            jdbc.queryForObject("$GET WHERE pk_task_error=?", MAPPER, id)
        }
        else {
            jdbc.queryForObject("$GET WHERE pk_task_error=? AND pk_organization=?", MAPPER, id, getOrgId())
        }
    }

    override fun getLast() : TaskError {
        return return if (hasPermission("zorroa::superadmin")) {
            jdbc.queryForObject("$GET ORDER BY time_created DESC LIMIT 1", MAPPER)
        }
        else {
            jdbc.queryForObject("$GET WHERE pk_organization=? ORDER BY time_created DESC LIMIT 1",
                    MAPPER, getOrgId())
        }
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM task_error WHERE pk_task_error=?", id) == 1
    }

    override fun deleteAll(job: JobId): Int {
        return jdbc.update("DELETE FROM task_error WHERE pk_job=?", job.jobId)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            TaskError(rs.getObject("pk_task_error") as UUID,
                    rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_asset") as UUID?,
                    rs.getString("str_path"),
                    rs.getString("str_message"),
                    rs.getString("str_processor"),
                    rs.getBoolean("bool_fatal"),
                    rs.getString("str_endpoint"),
                    rs.getString("str_phase"),
                    rs.getLong("time_created"))
        }

        private const val COUNT = "SELECT COUNT(1) FROM task_error INNER JOIN job ON (job.pk_job = task_error.pk_job)"

        private const val GET = "SELECT task_error.* FROM task_error INNER JOIN job ON (job.pk_job = task_error.pk_job)"

        private val INSERT = JdbcUtils.insert("task_error",
                "pk_task_error",
                "pk_task",
                "pk_job",
                "pk_asset",
                "str_message",
                "str_path",
                "str_processor",
                "str_endpoint",
                "str_extension",
                "bool_fatal",
                "str_phase",
                "time_created")
    }
}