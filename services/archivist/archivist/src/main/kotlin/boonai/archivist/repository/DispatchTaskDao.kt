package boonai.archivist.repository

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.domain.DispatchPriority
import boonai.archivist.domain.DispatchTask
import boonai.archivist.domain.JobState
import boonai.archivist.domain.PendingTasksStats
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.ZpsScript
import boonai.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface DispatchTaskDao {

    /**
     * Return the next N tasks for an Organization.
     *
     * @param projectId The unique Organization ID
     * @param count The maximum number of tasks to return
     */
    fun getNextByProject(projectId: UUID, count: Int = 10): List<DispatchTask>

    /**
     * Return the next N tasks with a minimum priority.  Lower is first.
     *
     * @param minPriority The minimum priority value.
     * @param count The maximum number of tasks to return
     */
    fun getNextByJobPriority(minPriority: Int, count: Int = 10): List<DispatchTask>

    /**
     * Return a list of DispatchPriority instances, sorted by by highest priority first.
     */
    fun getDispatchPriority(): List<DispatchPriority>

    /**
     * Return the number of pending tasks with `in progress` jobs
     */
    fun getPendingTasksStats(): PendingTasksStats
}

@Repository
class DispatchTaskDaoImpl : AbstractDao(), DispatchTaskDao {

    override fun getNextByProject(projectId: UUID, count: Int): List<DispatchTask> {
        return jdbc.query(
            GET_BY_PROJ, MAPPER,
            JobState.InProgress.ordinal,
            TaskState.Waiting.ordinal,
            projectId,
            count
        )
    }

    override fun getNextByJobPriority(minPriority: Int, count: Int): List<DispatchTask> {
        return jdbc.query(
            GET_BY_PRIORITY, MAPPER,
            JobState.InProgress.ordinal,
            TaskState.Waiting.ordinal,
            minPriority,
            count
        )
    }

    override fun getDispatchPriority(): List<DispatchPriority> {
        val result = jdbc.query(GET_DISPATCH_PRIORITY) { rs, _ ->
            DispatchPriority(
                rs.getObject("pk_project") as UUID,
                rs.getInt("priority")
            )
        }
        result.sortBy { it.priority }
        return result
    }

    override fun getPendingTasksStats(): PendingTasksStats {
        val result = jdbc.queryForObject(GET_PENDING_TASKS_STATS) { rs, _ ->
            PendingTasksStats(
                rs.getLong("pending_tasks"),
                rs.getLong("running_tasks"),
                rs.getLong("max_running_tasks")
            )
        }

        return result ?: PendingTasksStats()
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->

            val script = Json.Mapper.readValue<ZpsScript>(rs.getString("json_script"))
            val globalArgs = Json.Mapper.readValue<MutableMap<String, Any>>(rs.getString("json_args"))
            for ((key, value) in globalArgs) {
                script.setGlobalArg(key, value)
            }

            DispatchTask(
                rs.getObject("pk_task") as UUID,
                rs.getObject("pk_job") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getObject("pk_datasource") as UUID?,
                rs.getString("str_name"),
                TaskState.values()[rs.getInt("int_state")],
                rs.getString("str_host"),
                script,
                Json.Mapper.readValue(rs.getString("json_env")),
                globalArgs
            )
        }

        private const val GET_DISPATCH_PRIORITY =
            "SELECT " +
                "job.pk_project, " +
                "SUM(job_count.int_task_state_1) AS priority " +
                "FROM job " +
                "INNER JOIN job_count ON (job.pk_job = job_count.pk_job) " +
                "WHERE " +
                "job.int_state = 0 " +
                "AND " +
                "job_count.int_task_state_0 > 0 " +
                "GROUP BY " +
                "job.pk_project"

        private const val GET =
            "SELECT " +
                "job.pk_project," +
                "job.pk_datasource," +
                "job.json_env," +
                "job.json_args," +
                "task.pk_task," +
                "task.pk_job," +
                "task.str_name," +
                "task.int_state," +
                "task.int_run_count," +
                "task.json_script, " +
                "task.str_host " +
                "FROM " +
                "task " +
                "INNER JOIN job ON job.pk_job = task.pk_job " +
                "INNER JOIN job_count ON job.pk_job = job_count.pk_job " +
                "WHERE " +
                "job.int_state=? " +
                "AND " +
                "job.bool_paused='f' " +
                "AND " +
                "task.int_state=? " +
                "AND " +
                "job_count.int_max_running_tasks > job_count.int_task_state_1 + int_task_state_5  "

        /**
         * Provides FIFO scheduling by job. The order is:
         *
         * - job priority
         * - job time created
         * - task time created
         */
        private const val GET_BY_PROJ = GET +
            "AND " +
            "job.pk_project=? " +
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

        private const val GET_PENDING_TASKS_STATS =
            "SELECT SUM(job_count.int_task_state_0) as pending_tasks," +
                "SUM(job_count.int_task_state_1) as running_tasks," +
                "SUM(job_count.int_max_running_tasks) as max_running_tasks " +
                "FROM job AS job " +
                "INNER JOIN job_count AS job_count " +
                "USING (pk_job) " +
                "WHERE " +
                "job.int_state=0 AND " +
                "job.bool_paused=false AND " +
                "job_count.int_max_running_tasks > job_count.int_task_state_1 + int_task_state_5"
    }
}
