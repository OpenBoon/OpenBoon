package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.common.domain.DispatchTask
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.TaskState
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface DispatchTaskDao {
    fun getNext(count: Int): List<DispatchTask>
}

@Repository
class DispatchTaskDaoImpl : AbstractDao(), DispatchTaskDao {

    override fun getNext(count: Int): List<DispatchTask> {
        return jdbc.query(GET, MAPPER, JobState.Active.ordinal, TaskState.Waiting.ordinal, count)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            DispatchTask(rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    TaskState.values()[rs.getInt("int_state")],
                    rs.getString("str_host"),
                    Json.deserialize(rs.getString("json_script"), ZpsScript::class.java),
                    Json.deserialize(rs.getString("json_env"), object : TypeReference<MutableMap<String, String>>(){}),
                    Json.deserialize(rs.getString("json_args"), object : TypeReference<MutableMap<String, Object>>(){}),
                    rs.getObject("pk_user_created") as UUID)
        }

        private const val GET = "SELECT " +
                "job.pk_organization," +
                "job.json_env," +
                "job.json_args," +
                "job.pk_user_created," +
                "task.pk_task,"+
                "task.pk_job,"+
                "task.str_name,"+
                "task.int_state,"+
                "task.json_script, "+
                "task.str_host " +
                "FROM " +
                    "task INNER JOIN job ON job.pk_job = task.pk_job " +
                "WHERE " +
                    "job.int_state=? " +
                "AND " +
                    "task.int_state=? " +
                "ORDER BY job.int_priority,task.time_created LIMIT ?"
    }
}