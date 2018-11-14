package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.util.event
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface TaskDao {
    fun create(job: JobId, spec: TaskSpec): Task
    fun get(id: UUID) : Task
    fun setState(task: TaskId, newState: TaskState, oldState: TaskState?) : Boolean
    fun setHostEndpoint(task: TaskId, host: String)
    fun setExitStatus(task: TaskId, exitStatus: Int)
    fun getScript(id: UUID) : ZpsScript
    fun incrementAssetStats(task: TaskId, counts: BatchCreateAssetsResponse) : Boolean
    fun getAll(pager: Pager, filter: TaskFilter): PagedList<Task>
}

@Repository
class TaskDaoImpl : AbstractDao(), TaskDao {

    override fun create(job: JobId, spec: TaskSpec): Task {
        Preconditions.checkNotNull(spec.name)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, job.jobId)
            ps.setString(3, spec.name)
            ps.setLong(4, time)
            ps.setLong(5, time)
            ps.setLong(6, time)
            ps.setString(7, Json.serializeToString(spec.script, "{}"))
            ps
        }

        logger.event("create Task",
                mapOf("taskId" to id,
                        "jobId" to job.jobId,
                        "taskName" to spec.name))

        jdbc.update("INSERT INTO task_stat (pk_task, pk_job) VALUES (?,?)", id, job.jobId)
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
            logger.event("update Task",
                    mapOf("taskId" to task.taskId,
                            "newState" to newState.name,
                            "oldState" to oldState?.name))

            if (newState in START_STATES) {
                jdbc.update("UPDATE task SET time_started=?, time_stopped=-1 WHERE pk_task=?", time, task.taskId)
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
        val updated = jdbc.update(INC_STATS,
                counts.total,
                counts.createdAssetIds.size,
                counts.warningAssetIds.size,
                counts.erroredAssetIds.size,
                counts.replacedAssetIds.size,
                task.taskId) == 1

        if (updated) {
            logger.event("updated TaskAssetStats",
                    mapOf("taskId" to task.taskId,
                            "assetsCreated" to counts.createdAssetIds.size,
                            "assetsWarned" to counts.warningAssetIds.size,
                            "assetErrors" to counts.erroredAssetIds.size,
                            "assetsReplaced" to counts.replacedAssetIds.size))
        }
        return updated
    }

    override fun getAll(pager: Pager, filter: TaskFilter): PagedList<Task> {
        val q = filter.getCountQuery("SELECT COUNT(1) FROM task ")
        val count = jdbc.queryForObject(q, Long::class.java, *filter.getValues())

        return PagedList(pager.setTotalCount(count),
                jdbc.query<Task>(filter.getQuery(GET_TASKS, pager),
                        MAPPER, *filter.getValues(pager)))
    }

    companion object {

        private val START_STATES = setOf(TaskState.Running)

        private val STOP_STATES = setOf(TaskState.Failure, TaskState.Skipped, TaskState.Success)

        private val MAPPER = RowMapper { rs, _ ->
            Task(rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    TaskState.values()[rs.getInt("int_state")])
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
                "task.int_state " +
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
                "json_script::JSON")

        private val GET_TASKS = "SELECT " +
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

    }
}
