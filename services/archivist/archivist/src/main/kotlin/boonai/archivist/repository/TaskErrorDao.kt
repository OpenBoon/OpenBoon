package boonai.archivist.repository

import boonai.archivist.domain.InternalTask
import boonai.archivist.domain.JobId
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.archivist.domain.TaskError
import boonai.archivist.domain.TaskErrorEvent
import boonai.archivist.domain.TaskErrorFilter
import boonai.archivist.domain.TaskId
import boonai.archivist.security.getProjectId
import boonai.common.service.logging.MeterRegistryHolder.getTags
import boonai.common.service.logging.warnEvent
import boonai.archivist.util.FileUtils
import boonai.archivist.util.JdbcUtils
import boonai.common.service.logging.MeterRegistryHolder.meterRegistry
import boonai.common.util.Json
import boonai.common.util.readValueOrNull
import io.micrometer.core.instrument.Tag
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.UUID

interface TaskErrorDao {
    fun create(task: InternalTask, error: TaskErrorEvent): TaskError
    fun batchCreate(task: InternalTask, specs: List<TaskErrorEvent>): Int
    fun get(id: UUID): TaskError
    fun getLast(): TaskError
    fun count(filter: TaskErrorFilter): Long
    fun getAll(filter: TaskErrorFilter): KPagedList<TaskError>
    fun findOneTaskError(filter: TaskErrorFilter): TaskError
    fun delete(id: UUID): Boolean
    fun deleteAll(job: JobId): Int
    fun deleteAll(taskId: TaskId): Int
}

@Repository
class TaskErrorDaoImpl : AbstractDao(), TaskErrorDao {

    override fun create(task: InternalTask, spec: TaskErrorEvent): TaskError {

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, task.taskId)
            ps.setObject(3, task.jobId)
            ps.setObject(4, spec.assetId)
            ps.setString(5, spec.message)
            ps.setString(6, spec.path)
            ps.setString(7, spec.processor)
            ps.setString(8, "not-implemented")
            ps.setString(9, FileUtils.extension(spec.path))
            ps.setBoolean(10, spec.fatal)
            ps.setString(11, spec.phase)
            ps.setLong(12, time)
            ps.setString(13, Json.serializeToString(spec.stackTrace, null))
            ps.setObject(14, getKeywords(spec))
            ps
        }

        warnEvent(task, spec)

        return TaskError(
            id,
            task.taskId,
            task.jobId,
            task.dataSourceId,
            spec.assetId,
            spec.path,
            spec.message,
            spec.processor,
            spec.fatal,
            "not-implemented",
            spec.phase,
            time,
            spec.stackTrace
        )
    }

    override fun batchCreate(task: InternalTask, specs: List<TaskErrorEvent>): Int {
        if (specs.isEmpty()) {
            return 0
        }

        val time = System.currentTimeMillis()
        val result = jdbc.batchUpdate(
            INSERT,
            object : BatchPreparedStatementSetter {

                @Throws(SQLException::class)
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val spec = specs[i]
                    val id = uuid1.generate()
                    ps.setObject(1, id)
                    ps.setObject(2, task.taskId)
                    ps.setObject(3, task.jobId)
                    ps.setObject(4, spec.assetId)
                    ps.setString(5, spec.message)
                    ps.setString(6, spec.path)
                    ps.setString(7, spec.processor)
                    ps.setString(8, "not-implemented")
                    ps.setString(9, FileUtils.extension(spec.path))
                    ps.setBoolean(10, spec.fatal)
                    ps.setString(11, spec.phase)
                    ps.setLong(12, time)
                    ps.setString(13, Json.serializeToString(spec.stackTrace, null))
                    ps.setObject(14, getKeywords(spec))
                }

                override fun getBatchSize(): Int {
                    return specs.size
                }
            }
        )

        specs.forEach { warnEvent(task, it) }
        return result.sum()
    }

    override fun count(filter: TaskErrorFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    override fun getAll(filter: TaskErrorFilter): KPagedList<TaskError> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun findOneTaskError(filter: TaskErrorFilter): TaskError {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }

    override fun get(id: UUID): TaskError {
        return jdbc.queryForObject(
            "$GET WHERE pk_task_error=? AND job.pk_project=?",
            MAPPER, id, getProjectId()
        )
    }

    override fun getLast(): TaskError {
        return jdbc.queryForObject(
            "$GET WHERE job.pk_project=? ORDER BY time_created DESC LIMIT 1",
            MAPPER, getProjectId()
        )
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM task_error WHERE pk_task_error=?", id) == 1
    }

    override fun deleteAll(job: JobId): Int {
        return jdbc.update("DELETE FROM task_error WHERE pk_job=?", job.jobId)
    }

    override fun deleteAll(task: TaskId): Int {
        return jdbc.update("DELETE FROM task_error WHERE pk_task=?", task.taskId)
    }

    fun getKeywords(spec: TaskErrorEvent): String {
        var keywords = JdbcUtils.getTsWordVector(spec.path, spec.processor, spec.message)
        spec.path?.let {
            keywords += " ${FileUtils.filename(it)}"
            keywords += " $it"
        }
        return keywords
    }

    fun warnEvent(task: InternalTask, spec: TaskErrorEvent) {
        meterRegistry.counter(
            "zorroa.task_errors",
            getTags(Tag.of("processor", spec.processor ?: "no-processor"))
        )

        logger.warnEvent(
            LogObject.TASK_ERROR, LogAction.CREATE, spec.message,
            mapOf(
                "assetId" to spec.assetId,
                "taskId" to task.taskId,
                "processor" to spec.processor,
                "jobId" to task.jobId
            )
        )
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            TaskError(
                rs.getObject("pk_task_error") as UUID,
                rs.getObject("pk_task") as UUID,
                rs.getObject("pk_job") as UUID,
                rs.getObject("pk_datasource") as UUID?,
                rs.getString("asset_id"),
                rs.getString("str_path"),
                rs.getString("str_message"),
                rs.getString("str_processor"),
                rs.getBoolean("bool_fatal"),
                rs.getString("str_endpoint"),
                rs.getString("str_phase"),
                rs.getLong("time_created"),
                Json.Mapper.readValueOrNull(rs.getString("json_stack_trace"))
            )
        }

        private const val COUNT = "SELECT COUNT(1) FROM " +
            "task_error " +
            "INNER JOIN " +
            "job ON (job.pk_job = task_error.pk_job)"

        private const val GET = "SELECT " +
            "task_error.*, " +
            "job.pk_datasource " +
            "FROM " +
            "task_error " +
            "INNER JOIN " +
            "job ON (job.pk_job = task_error.pk_job)"

        private val INSERT = JdbcUtils.insert(
            "task_error",
            "pk_task_error",
            "pk_task",
            "pk_job",
            "asset_id",
            "str_message",
            "str_path",
            "str_processor",
            "str_endpoint",
            "str_extension",
            "bool_fatal",
            "str_phase",
            "time_created",
            "json_stack_trace::jsonb",
            "fti_keywords@to_tsvector"
        )
    }
}
