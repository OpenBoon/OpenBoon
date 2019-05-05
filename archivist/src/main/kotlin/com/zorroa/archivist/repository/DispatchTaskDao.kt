package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.domain.DispatchPriority
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.common.domain.DispatchTask
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.TaskState
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface DispatchTaskDao {

    /**
     * Return the next N tasks for an Organization.
     *
     * @param organizationId The unique Organization ID
     * @param count The maximum number of tasks to return
     */
    fun getNextByOrg(organizationId: UUID, count: Int=10): List<DispatchTask>

    /**
     * Return the next N tasks with a minimum priority.
     *
     * @param count The maximum number of tasks to return
     */
    fun getNextByJobPriority(minPriority: Int, count: Int=10): List<DispatchTask>

    /**
     * Return a list of DispatchPriority instances, sorted by by highest priority first.
     */
    fun getDispatchPriority(): List<DispatchPriority>
}

@Repository
class DispatchTaskDaoImpl : AbstractDao(), DispatchTaskDao {

    override fun getNextByOrg(organizationId: UUID, count: Int): List<DispatchTask> {
        return jdbc.query(GET_BY_ORG, MAPPER,
                JobState.Active.ordinal,
                TaskState.Waiting.ordinal,
                organizationId,
                count)
    }

    override fun getNextByJobPriority(minPriority: Int, count: Int): List<DispatchTask> {
        return jdbc.query(GET_BY_PRIORITY, MAPPER,
            JobState.Active.ordinal,
            TaskState.Waiting.ordinal,
            minPriority,
            count)
    }

    override fun getDispatchPriority(): List<DispatchPriority> {
        val result = jdbc.query(GET_DISPATCH_PRIORITY) { rs, _ ->
            DispatchPriority(
                    rs.getObject("pk_organization") as UUID,
                    rs.getInt("priority"))
        }
        result.sortBy { it.priority }
        return result
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
                    Json.deserialize(rs.getString("json_env"),
                            object : TypeReference<MutableMap<String, String>>(){}),
                    Json.deserialize(rs.getString("json_args"),
                            object : TypeReference<MutableMap<String, Any>>(){}),
                    rs.getObject("pk_user_created") as UUID)
        }

        private const val GET_DISPATCH_PRIORITY =
            "SELECT " +
                "job.pk_organization, " +
                "SUM(job_count.int_task_state_1) AS priority " +
            "FROM job " +
                "INNER JOIN job_count ON (job.pk_job = job_count.pk_job) " +
            "WHERE " +
                "job.int_state = 0 " +
            "AND " +
                "job_count.int_task_state_0 > 0 " +
            "GROUP BY " +
                "job.pk_organization"

        private const val GET =
            "SELECT " +
                "job.pk_organization," +
                "job.json_env," +
                "job.json_args," +
                "job.pk_user_created," +
                "task.pk_task,"+
                "task.pk_job,"+
                "task.str_name,"+
                "task.int_state,"+
                "task.int_run_count,"+
                "task.json_script, "+
                "task.str_host " +
            "FROM " +
                "task INNER JOIN job ON job.pk_job = task.pk_job " +
            "WHERE " +
                "job.int_state=? " +
            "AND " +
                "job.bool_paused='f' " +
            "AND " +
                "task.int_state=? "


        /**
         * Provides FIFO scheduling by job. The order is:
         *
         * - job priority
         * - job time created
         * - task time created
         */
        private const val GET_BY_ORG = GET +
            "AND " +
                "job.pk_organization=? " +
            "ORDER BY " +
                "job.int_priority,job.time_created,task.time_created LIMIT ?"

        /**
         * Provides FIFO scheduling by high priority job, not org filtered.
         *
         * - job priority
         * - job time created
         * - task time created
         */
        private const val GET_BY_PRIORITY = GET +
            "AND " +
                "job.int_priority <= ? " +
            "ORDER BY " +
                "job.int_priority,job.time_created,task.time_created LIMIT ?"
    }
}