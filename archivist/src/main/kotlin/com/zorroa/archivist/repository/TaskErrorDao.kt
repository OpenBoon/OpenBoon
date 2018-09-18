package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.domain.*
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface TaskErrorDao {
    fun create(event: TaskEvent, error: TaskErrorEvent): TaskError
    fun get(id: UUID) : TaskError
    fun getLast() : TaskError
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
            ps.setString(8, event.endpoint)
            ps.setString(9, FileUtils.extension(spec.path))
            ps.setBoolean(10, spec.fatal)
            ps.setLong(11, time)
            ps
        }
        return get(id)
    }

    override fun get(id: UUID) : TaskError {
        return jdbc.queryForObject("$GET WHERE pk_task_error=?", MAPPER, id)
    }

    override fun getLast() : TaskError {
        return jdbc.queryForObject("$GET ORDER BY time_created DESC LIMIT 1", MAPPER)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            TaskError(rs.getObject("pk_task_error") as UUID,
                    rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_asset") as UUID,
                    rs.getString("str_path"),
                    rs.getString("str_message"),
                    rs.getString("str_processor"),
                    rs.getBoolean("bool_fatal"),
                    rs.getString("str_endpoint"))
        }

        private const val GET = "SELECT * FROM task_error"

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
                "time_created")
    }
}