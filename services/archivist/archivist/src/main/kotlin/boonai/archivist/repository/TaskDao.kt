package boonai.archivist.repository

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Preconditions
import boonai.archivist.domain.AssetCounters
import boonai.archivist.domain.InternalTask
import boonai.archivist.domain.JobId
import boonai.archivist.domain.JobState
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.archivist.domain.Task
import boonai.archivist.domain.TaskFilter
import boonai.archivist.domain.TaskId
import boonai.archivist.domain.TaskSpec
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.ZpsScript
import boonai.common.service.logging.MeterRegistryHolder.getTags
import boonai.common.service.logging.event
import boonai.archivist.util.JdbcUtils
import boonai.common.service.logging.MeterRegistryHolder.meterRegistry
import boonai.common.util.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Duration
import java.util.UUID

interface TaskDao {
    fun create(job: JobId, spec: TaskSpec): Task
    fun get(id: UUID): Task
    fun getInternal(id: UUID): InternalTask
    fun setState(task: TaskId, newState: TaskState, oldState: TaskState?): Boolean
    fun setHostEndpoint(task: TaskId, host: String)
    fun getHostEndpoint(taskId: TaskId): String?
    fun setExitStatus(task: TaskId, exitStatus: Int)
    fun getScript(id: UUID): ZpsScript
    fun incrementAssetCounters(task: TaskId, counts: AssetCounters): Boolean
    fun getAll(tf: TaskFilter?): KPagedList<Task>
    fun getAll(job: UUID, state: TaskState): List<InternalTask>
    fun isAutoRetryable(task: TaskId): Boolean
    fun setProgress(task: TaskId, progress: Int)
    fun setStatus(task: TaskId, status: String)

    /**
     * Return the total number of pending tasks.
     */
    fun getPendingTaskCount(): Long

    /**
     * Update the task's ping time as long as it's running on the given endpoint.
     */
    fun updatePingTime(taskId: UUID, endpoint: String): Boolean

    /**
     * Return a list of [InternalTask]s which have not seen a ping for the given [Duration]
     */
    fun getOrphans(duration: Duration): List<InternalTask>

    fun findOne(filter: TaskFilter): Task
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
            ps.setLong(7, time)
            ps.setString(8, Json.serializeToString(spec.script?.copyWithoutChildren(), "{}"))
            ps.setInt(9, 0)
            ps
        }

        val totalAssets = spec.script?.assets?.size ?: 0
        jdbc.update(
            "INSERT INTO task_stat (pk_task, pk_job,int_asset_total_count) VALUES (?,?,?)",
            id, job.jobId, totalAssets
        )
        logger.event(
            LogObject.TASK, LogAction.CREATE,
            mapOf("taskId" to id, "taskName" to spec.name, "assetCount" to totalAssets)
        )
        return get(id)
    }

    override fun get(id: UUID): Task {
        return jdbc.queryForObject("$GET WHERE task.pk_task=?", MAPPER, id)
    }

    override fun getInternal(id: UUID): InternalTask {
        return jdbc.queryForObject("$GET_INTERNAL WHERE task.pk_task=?", INTERNAL_MAPPER, id)
    }

    override fun getScript(id: UUID): ZpsScript {
        val script = jdbc.queryForObject(
            "SELECT json_script FROM task WHERE pk_task=?",
            String::class.java, id
        )
        return Json.Mapper.readValue(script)
    }

    override fun setState(task: TaskId, newState: TaskState, oldState: TaskState?): Boolean {
        val time = System.currentTimeMillis()
        // Note: There is a trigger updating counts here.
        val updated = if (oldState != null) {
            jdbc.update(
                "UPDATE task SET int_state=?,time_modified=? WHERE pk_task=? AND int_state=?",
                newState.ordinal, time, task.taskId, oldState.ordinal
            ) == 1
        } else {
            jdbc.update(
                "UPDATE task SET int_state=?,time_modified=? WHERE pk_task=?",
                newState.ordinal, time, task.taskId
            ) == 1
        }

        if (updated) {
            meterRegistry.counter("zorroa.task.state", getTags(newState.metricsTag())).increment()
            logger.event(
                LogObject.TASK, LogAction.STATE_CHANGE,
                mapOf(
                    "taskId" to task.taskId,
                    "newState" to newState.name,
                    "oldState" to oldState?.name
                )
            )

            when (newState) {
                in RESET_STATES -> {
                    jdbc.update(
                        "UPDATE task SET time_started=-1, time_stopped=-1, int_progress=0 WHERE pk_task=?",
                        task.taskId
                    )
                }
                in START_STATES -> {
                    jdbc.update(
                        "UPDATE task SET time_ping=?, time_started=?, int_run_count=int_run_count+1, " +
                            "time_stopped=-1,int_ping_count=1, int_progress=0 WHERE pk_task=?",
                        time, time, task.taskId
                    )
                }
                in STOP_STATES -> {
                    jdbc.update(
                        "UPDATE task SET time_stopped=?, int_progress=100 WHERE pk_task=?",
                        time, task.taskId
                    )
                }
            }
        }

        return updated
    }

    override fun setHostEndpoint(task: TaskId, host: String) {
        jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", host, task.taskId)
    }

    override fun getHostEndpoint(taskId: TaskId): String? {
        return try {
            jdbc.queryForObject(
                "SELECT str_host FROM task WHERE pk_task=?",
                String::class.java, taskId.taskId
            )
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun setExitStatus(task: TaskId, exitStatus: Int) {
        jdbc.update(
            "UPDATE task SET int_exit_status=? WHERE pk_task=?",
            exitStatus, task.taskId
        )
    }

    override fun setProgress(task: TaskId, progress: Int) {
        jdbc.update(
            "UPDATE task SET int_progress=? WHERE pk_task=?",
            progress, task.taskId
        )
    }

    override fun setStatus(task: TaskId, status: String) {
        jdbc.update(
            "UPDATE task SET str_status=? WHERE pk_task=?",
            status, task.taskId
        )
    }

    override fun incrementAssetCounters(task: TaskId, counts: AssetCounters): Boolean {
        return jdbc.update(
            ASSET_COUNTS_INC,
            counts.total,
            task.taskId
        ) == 1
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

    override fun getAll(job: UUID, state: TaskState): List<InternalTask> {
        return jdbc.query(
            "$GET_INTERNAL WHERE task.pk_job=? AND task.int_state=?",
            INTERNAL_MAPPER, job, state.ordinal
        )
    }

    override fun findOne(filter: TaskFilter): Task {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }

    override fun isAutoRetryable(task: TaskId): Boolean {
        return jdbc.queryForObject(
            "SELECT int_run_count <= ? FROM task where pk_task=?",
            Boolean::class.java, autoRetryLimit, task.taskId
        )
    }

    override fun getPendingTaskCount(): Long {
        return jdbc.queryForObject(
            GET_PENDING_COUNT,
            Long::class.java, JobState.InProgress.ordinal, TaskState.Waiting.ordinal
        )
    }

    override fun updatePingTime(taskId: UUID, endpoint: String): Boolean {
        return jdbc.update(
            UPDATE_PING,
            System.currentTimeMillis(), taskId, endpoint, TaskState.Running.ordinal
        ) == 1
    }

    override fun getOrphans(duration: Duration): List<InternalTask> {
        val time = System.currentTimeMillis() - duration.toMillis()
        return jdbc.query(
            GET_ORPHANS, INTERNAL_MAPPER,
            TaskState.Running.ordinal,
            TaskState.Queued.ordinal,
            time
        )
    }

    companion object {

        private val RESET_STATES = setOf(TaskState.Waiting)

        private val START_STATES = setOf(TaskState.Running)

        private val STOP_STATES = setOf(TaskState.Failure, TaskState.Skipped, TaskState.Success)

        private val INTERNAL_MAPPER = RowMapper { rs, _ ->
            InternalTask(
                rs.getObject("pk_task") as UUID,
                rs.getObject("pk_job") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getObject("pk_datasource") as UUID?,
                rs.getString("str_name"),
                TaskState.values()[rs.getInt("int_state")]
            )
        }

        private val MAPPER = RowMapper { rs, _ ->
            Task(
                rs.getObject("pk_task") as UUID,
                rs.getObject("pk_job") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getObject("pk_datasource") as UUID?,
                rs.getString("str_name"),
                TaskState.values()[rs.getInt("int_state")],
                rs.getString("str_host"),
                rs.getLong("time_started"),
                rs.getLong("time_stopped"),
                rs.getLong("time_created"),
                rs.getLong("time_ping"),
                buildAssetCounts(rs),
                rs.getInt("int_progress"),
                rs.getInt("int_run_count"),
                rs.getString("str_status")
            )
        }

        private inline fun buildAssetCounts(rs: ResultSet): Map<String, Int> {
            val result = mutableMapOf<String, Int>()
            result["assetCreatedCount"] = 0
            result["assetReplacedCount"] = 0
            result["assetWarningCount"] = rs.getInt("int_asset_warning_count")
            result["assetErrorCount"] = rs.getInt("int_asset_error_count")
            result["assetTotalCount"] = rs.getInt("int_asset_total_count")
            return result
        }

        private const val UPDATE_PING =
            "UPDATE " +
                "task " +
                "SET " +
                "int_ping_count=int_ping_count+1," +
                "time_ping=? " +
                "WHERE " +
                "pk_task=? " +
                "AND " +
                "str_host=? " +
                "AND " +
                "int_state=?"

        private const val ASSET_COUNTS_INC = "UPDATE " +
            "task_stat " +
            "SET " +
            "int_asset_total_count=int_asset_total_count+? " +
            "WHERE " +
            "pk_task=?"

        private const val GET_INTERNAL = "SELECT " +
            "task.pk_task," +
            "task.pk_job," +
            "task.str_name," +
            "task.int_state, " +
            "job.pk_project, " +
            "job.pk_datasource " +
            "FROM " +
            "task INNER JOIN job ON (task.pk_job = job.pk_job)"

        private const val COUNT = "SELECT COUNT(1) " +
            "FROM " +
            "task " +
            "INNER JOIN " +
            "job ON (task.pk_job = job.pk_job) "

        private const val GET_ORPHANS =
            "$GET_INTERNAL " +
                "WHERE " +
                "task.int_state IN (?,?) AND task.time_ping < ? LIMIT 15"

        private val INSERT = JdbcUtils.insert(
            "task",
            "pk_task",
            "pk_job",
            "str_name",
            "time_created",
            "time_modified",
            "time_state_change",
            "time_ping",
            "json_script::JSON",
            "int_run_count"
        )

        private const val GET = "SELECT " +
            "task.pk_task," +
            "task.pk_parent," +
            "task.pk_job," +
            "task.str_name," +
            "task.int_state," +
            "task.int_order," +
            "task.time_started," +
            "task.time_stopped," +
            "task.time_created," +
            "task.time_ping," +
            "task.time_state_change," +
            "task.int_exit_status," +
            "task.str_host, " +
            "task.int_run_count, " +
            "task.int_progress, " +
            "task.str_status, " +
            "task_stat.int_asset_total_count," +
            "task_stat.int_asset_create_count," +
            "task_stat.int_asset_replace_count," +
            "task_stat.int_asset_error_count," +
            "task_stat.int_asset_warning_count," +
            "job.pk_project, " +
            "job.pk_datasource " +
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
