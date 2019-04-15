package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.service.MeterRegistryHolder
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.archivist.service.event
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.util.Json
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*
import javax.annotation.PostConstruct

interface TaskDao {
    fun create(job: JobId, spec: TaskSpec): Task
    fun get(id: UUID) : Task
    fun setState(task: TaskId, newState: TaskState, oldState: TaskState?) : Boolean
    fun setHostEndpoint(task: TaskId, host: String)
    fun setExitStatus(task: TaskId, exitStatus: Int)
    fun getScript(id: UUID) : ZpsScript
    fun incrementAssetStats(task: TaskId, counts: BatchCreateAssetsResponse) : Boolean
    fun getAll(tf: TaskFilter?): KPagedList<Task>
    fun getAll(job: UUID, state: TaskState): List<Task>
    fun isAutoRetryable(task: TaskId): Boolean

    /**
     * Return the total number of pending tasks.
     */
    fun getPendingTaskCount(): Long
}

@Repository
class TaskDaoImpl : AbstractDao(), TaskDao {

    @Value("\${archivist.dispatcher.autoRetryLimit}")
    lateinit var autoRetryLimit: Number

    override fun create(job: JobId, spec: TaskSpec): Task {
        Preconditions.checkNotNull(spec.name)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, job.jobId)
            ps.setString(3, spec.name.trim())
            ps.setLong(4, time)
            ps.setLong(5, time)
            ps.setLong(6, time)
            ps.setString(7, Json.serializeToString(spec.script, "{}"))
            ps.setInt(8, 0)
            ps
        }

        jdbc.update("INSERT INTO task_stat (pk_task, pk_job) VALUES (?,?)", id, job.jobId)
        logger.event(LogObject.TASK, LogAction.CREATE,
                mapOf("taskId" to id, "taskName" to spec.name))
        return get(id)
    }

    override fun get(id: UUID) : Task {
        return jdbc.queryForObject("$GET WHERE pk_task=?", MAPPER, id)
    }

    override fun getScript(id: UUID) : ZpsScript {
        val script = jdbc.queryForObject("SELECT json_script FROM task WHERE pk_task=?", String::class.java, id)
        return Json.deserialize(script, ZpsScript::class.java)
    }

    override fun setState(task: TaskId, newState: TaskState, oldState: TaskState?) : Boolean {
        val time = System.currentTimeMillis()
        // Note: There is a trigger updating counts here.
        val updated =  if (oldState != null) {
            jdbc.update("UPDATE task SET int_state=?,time_modified=? WHERE pk_task=? AND int_state=?",
                    newState.ordinal, time, task.taskId, oldState.ordinal) == 1
        }
        else {
            jdbc.update("UPDATE task SET int_state=?,time_modified=? WHERE pk_task=?",
                    newState.ordinal, time, task.taskId) == 1
        }
        if (updated) {
            meterRegistry.counter("zorroa.task.state", getTags(newState.metricsTag())).increment()
            logger.event(LogObject.TASK, LogAction.STATE_CHANGE,
                    mapOf("taskId" to task.taskId,
                            "newState" to newState.name,
                            "oldState" to oldState?.name))

            if (newState in START_STATES) {
                jdbc.update("UPDATE task SET time_started=?, int_run_count=int_run_count+1, time_stopped=-1 WHERE pk_task=?", time, task.taskId)
            }
            else if (newState in STOP_STATES) {
                jdbc.update("UPDATE task SET time_stopped=? WHERE pk_task=?", time, task.taskId)
            }
        }

        return updated
    }

    override fun setHostEndpoint(task: TaskId, host: String) {
        jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", host, task.taskId)
    }

    override fun setExitStatus(task: TaskId, exitStatus: Int) {
        jdbc.update("UPDATE task SET int_exit_status=? WHERE pk_task=?", exitStatus, task.taskId)
    }

    override fun incrementAssetStats(task: TaskId, counts: BatchCreateAssetsResponse) : Boolean {
        return jdbc.update(INC_STATS,
                counts.total,
                counts.createdAssetIds.size,
                counts.warningAssetIds.size,
                counts.erroredAssetIds.size,
                counts.replacedAssetIds.size,
                task.taskId) == 1
    }

    override fun getAll(tf: TaskFilter?): KPagedList<Task> {
        val filter = tf ?: TaskFilter()
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private fun count(filter: TaskFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    override fun getAll(job: UUID, state: TaskState): List<Task> {
        return jdbc.query<Task>("$GET_TASKS WHERE task.pk_job=? AND task.int_state=?",
                MAPPER, job, state.ordinal)
    }

    override fun isAutoRetryable(task: TaskId): Boolean {
        return jdbc.queryForObject("SELECT int_run_count <= ? FROM task where pk_task=?",
                Boolean::class.java, autoRetryLimit, task.taskId)
    }

    override fun getPendingTaskCount(): Long {
        return jdbc.queryForObject(GET_PENDING_COUNT,
                Long::class.java, JobState.Active.ordinal, TaskState.Waiting.ordinal)
    }

    companion object {

        private val START_STATES = setOf(TaskState.Running)

        private val STOP_STATES = setOf(TaskState.Failure, TaskState.Skipped, TaskState.Success)

        private val MAPPER = RowMapper { rs, _ ->
            Task(rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    TaskState.values()[rs.getInt("int_state")],
                    rs.getString("str_host"))
        }

        private const val INC_STATS = "UPDATE " +
                "task_stat " +
                "SET " +
                "int_asset_total_count=int_asset_total_count+?," +
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?," +
                "int_asset_replace_count=int_asset_replace_count+? " +
                "WHERE " +
                "pk_task=?"
    
        private const val GET = "SELECT " +
                "job.pk_organization, " +
                "task.pk_task," +
                "task.pk_job," +
                "task.str_name," +
                "task.int_state, " +
                "task.str_host, " +
                "task.int_run_count " +
                "FROM " +
                "task INNER JOIN job ON (job.pk_job=task.pk_job) "


        private const val COUNT = "SELECT COUNT(1) FROM task"

        private val INSERT = JdbcUtils.insert("task",
                "pk_task",
                "pk_job",
                "str_name",
                "time_created",
                "time_modified",
                "time_state_change",
                "json_script::JSON",
                "int_run_count")

        private const val GET_TASKS = "SELECT " +
                "task.pk_task," +
                "task.pk_parent," +
                "task.pk_job," +
                "task.str_name," +
                "task.int_state," +
                "task.int_order," +
                "task.time_started," +
                "task.time_stopped," +
                "task.time_created," +
                "task.time_state_change," +
                "task.int_exit_status," +
                "task.str_host, " +
                "task.int_run_count, " +
                "task_stat.int_asset_total_count," +
                "task_stat.int_asset_create_count," +
                "task_stat.int_asset_replace_count," +
                "task_stat.int_asset_error_count," +
                "task_stat.int_asset_warning_count," +
                "job.pk_organization "+
                "FROM " +
                "task " +
                "JOIN task_stat ON task.pk_task = task_stat.pk_task " +
                "JOIN job ON task.pk_job = job.pk_job "

        private const val GET_PENDING_COUNT =
                "SELECT " +
                    "COUNT(1) " +
                "FROM " +
                    "job," +
                    "task " +
                "WHERE " +
                    "job.pk_job = task.pk_job " +
                "AND " +
                    "job.int_state = ? " +
                "AND " +
                    "task.int_state = ? "

    }
}
