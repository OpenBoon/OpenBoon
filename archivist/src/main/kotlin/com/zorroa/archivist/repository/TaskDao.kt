package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface TaskDao {

    fun create(spec: TaskSpec): Task

    fun get(id: UUID): Task

}

/**
 * Created by chambers on 7/11/16.
 */
@Repository
class TaskDaoImpl : AbstractDao(), TaskDao {

    override fun create(spec: TaskSpec): Task {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val userid = getUserId()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.assetId)
            ps.setObject(3, getUser().organizationId)
            ps.setObject(4, spec.pipelineId)
            ps.setLong(5, 1) // hardcoded for now
            ps.setInt(6, TaskState.WAITING.ordinal)
            ps.setString(7, spec.name)
            ps.setLong(8, time)
            ps.setLong(9, time)
            ps.setObject(10, userid)
            ps.setObject(11, userid)
            ps.setLong(12, System.nanoTime())
            ps
        })

        return get(id)
    }

    override fun get(id: UUID): Task {
        return jdbc.queryForObject<Task>("$GET WHERE task.pk_task=?", MAPPER, id)
    }

    companion object {

        private const val GET = "SELECT " +
                "pk_task,"+
                "pk_asset,"+
                "pk_pipeline,"+
                "pk_organization,"+
                "int_version,"+
                "str_name " +
            "FROM " +
                "task "

        private val INSERT = JdbcUtils.insert("task",
                "pk_task",
                "pk_asset",
                "pk_organization",
                "pk_pipeline",
                "int_version",
                "int_state",
                "str_name",
                "time_created",
                "time_modified",
                "pk_user_created",
                "pk_user_modified",
                "int_order")

        private val MAPPER = RowMapper<Task> { rs, _ ->
            Task(
                    rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_asset") as UUID,
                    rs.getObject("pk_pipeline") as? UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getLong("int_version"),
                    rs.getString("str_name"))
        }
    }
}

