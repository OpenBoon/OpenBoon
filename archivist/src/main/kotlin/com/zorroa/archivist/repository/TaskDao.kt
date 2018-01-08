package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.common.cluster.thrift.TaskStartT
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.common.domain.TaskState
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

interface TaskDao {

    fun create(spec: TaskSpec): Task

    fun setHost(task: TaskId, host: String): Boolean

    fun setExitStatus(task: TaskId, exitStatus: Int): Boolean

    fun incrementStats(id: Int, addr: TaskStatsAdder): Boolean

    fun clearStats(id: Int): Boolean

    fun getState(task: TaskId, forUpdate: Boolean): TaskState

    fun getExecutableTask(id: Int): TaskStartT

    fun setState(script: TaskId, value: TaskState, vararg expect: TaskState): Boolean

    fun decrementDependCount(task: TaskId): Int

    fun incrementDependCount(task: TaskId): Int

    fun createParentDepend(child: TaskId): Boolean

    fun getWaiting(limit: Int): List<TaskStartT>

    fun getOrphanTasks(limit: Int, duration: Long, unit: TimeUnit): List<Task>

    fun getAll(job: Int, page: Pager): PagedList<Task>

    fun getAll(job: Int, pager: Pager, filter: TaskFilter): PagedList<Task>

    fun getAll(job: Int, state: TaskState): List<Task>

    fun getAll(job: Int, filter: TaskFilter): List<Task>

    fun get(id: Int): Task

    fun countByJob(job: Int): Long

    /**
     * Update's a tasks ping time.
     * @param taskIds
     */
    fun updatePingTime(taskIds: List<Int>): Int

    companion object {

        val STOPPERS: Set<TaskState> = Sets.newEnumSet(ImmutableList.of(
                TaskState.Skipped, TaskState.Failure, TaskState.Success), TaskState::class.java)

        val STARTERS: Set<TaskState> = Sets.newEnumSet(ImmutableList.of(
                TaskState.Running), TaskState::class.java)

        val RESET: Set<TaskState> = Sets.newEnumSet(ImmutableList.of(
                TaskState.Waiting), TaskState::class.java)
    }
}

/**
 * Created by chambers on 7/11/16.
 */
@Repository
open class TaskDaoImpl : AbstractDao(), TaskDao {

    @Autowired
    internal var shared: SharedData? = null

    @Autowired
    internal var networkEnv: NetworkEnvironment? = null

    private var sharedPath: String? = null

    private val EXECUTE_TASK_MAPPER = RowMapper<TaskStartT> { rs, _ ->
        /*
         * We don't parse the script here, its not needed as we're just going to
         * turn it back into a string anyway.
         */
        val workDir = rs.getString("str_root_path")

        val t = TaskStartT()
        t.setId(rs.getInt("pk_task"))
        t.setJobId(rs.getInt("pk_job"))
        t.setName(rs.getString("str_name"))
        if (rs.getObject("pk_parent") != null) {
            t.setParent(rs.getInt("pk_parent"))
        }

        t.setEnv(Json.deserialize(rs.getString("json_env"), Json.STRING_MAP))
        t.getEnv().put("ZORROA_JOB_ID", t.getJobId().toString())
        t.getEnv().put("ZORROA_TASK_ID", t.getId().toString())
        t.getEnv().put("ZORROA_USER", rs.getString("str_username"))
        t.getEnv().put("ZORROA_HMAC_KEY", rs.getString("hmac_key"))
        t.getEnv().put("ZORROA_WORK_DIR", workDir)

        t.setArgMap(rs.getString("json_args").toByteArray())

        t.setWorkDir(workDir)
        t.setSharedDir(sharedPath)
        t.setMasterHost(networkEnv!!.clusterAddr)
        t.setScriptPath(scriptPath(workDir, t.getName(), t.getId()))
        t.setLogPath(logPath(workDir, t.getName(), t.getId()))
        t.setOrder(rs.getInt("int_order"))
        t
    }

    @PostConstruct
    fun init() {
        sharedPath = shared!!.root.toString()
    }

    override fun create(task: TaskSpec): Task {
        val time = System.currentTimeMillis()
        /**
         * TODO: because we insert to get the ID, the ID stored on the script
         * is inaccurate.  Currently we just handle this in the mapper
         * but we could manually query the sequence
         */
        Preconditions.checkNotNull(task.jobId)
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_task"))
            ps.setInt(1, task.jobId!!)
            ps.setObject(2, task.parentTaskId)
            ps.setString(3, if (task.name == null) "subtask" else task.name)
            ps.setInt(4, TaskState.Waiting.ordinal)
            ps.setInt(5, task.order)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setLong(8, time)
            ps
        }, keyHolder)
        val id = keyHolder.key.toInt()

        jdbc.update("INSERT INTO task_stat (pk_task, pk_job, int_asset_total_count) VALUES (?, ?, ?)",
                id, task.jobId, task.assetCount)
        return get(id)
    }

    override fun setHost(task: TaskId, host: String): Boolean {
        return jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", host, task.taskId) == 1
    }

    override fun setExitStatus(task: TaskId, exitStatus: Int): Boolean {
        return jdbc.update("UPDATE task SET int_exit_status=? WHERE pk_task=?",
                exitStatus, task.taskId) == 1
    }

    override fun incrementStats(id: Int, addr: TaskStatsAdder): Boolean {
        return jdbc.update(INC_STATS, addr.create, addr.update, addr.warning, addr.error, addr.replace, id) == 1
    }

    override fun clearStats(id: Int): Boolean {
        return jdbc.update(CLEAR_STATS, id) == 1
    }

    override fun getState(task: TaskId, forUpdate: Boolean): TaskState {
        val q = "SELECT int_state FROM TASK WHERE pk_task=?"
        if (forUpdate) {
            q + " FOR UPDATE"
        }
        return TaskState.values()[jdbc.queryForObject(q, Int::class.java, task.taskId)]
    }

    override fun getExecutableTask(id: Int): TaskStartT {
        return jdbc.queryForObject<TaskStartT>(GET_TASK_TO_EXECUTE + " AND task.pk_task=?", EXECUTE_TASK_MAPPER, id)
    }

    override fun setState(task: TaskId, value: TaskState, vararg expect: TaskState): Boolean {
        logger.debug("setting task: {} from {} to {}", task.taskId, expect, value)
        val values = mutableListOf<Any>()
        val fields = mutableListOf<Any>()
        val time = System.currentTimeMillis()

        fields.add("int_state=?")
        values.add(value.ordinal)
        fields.add("time_state_change=?")
        values.add(time)
        fields.add("time_ping=?")
        values.add(time)

        when {
            TaskDao.STOPPERS.contains(value) -> {
                fields.add("time_stopped=?")
                values.add(time)
            }
            TaskDao.STARTERS.contains(value) -> {
                fields.add("time_stopped=-1")
                fields.add("time_started=?")
                values.add(time)
            }
            TaskDao.RESET.contains(value) -> {
                fields.add("time_stopped=-1")
                fields.add("time_started=-1")
            }
        }

        values.add(task.taskId)

        val sb = StringBuilder(256)
        sb.append("UPDATE task SET ")
        sb.append(fields.joinToString(","))
        sb.append(" WHERE pk_task=? ")

        if (expect.isNotEmpty()) {
            expect.mapTo(values) { it.ordinal }
            sb.append("AND " + JdbcUtils.`in`("int_state", expect.size))
        } else {
            values.add(value.ordinal)
            sb.append(" AND int_state != ?")
        }

        return jdbc.update(sb.toString(), *values.toTypedArray()) == 1
    }

    override fun decrementDependCount(finishedTask: TaskId): Int {
        // Decrement tasks depending on both ourself and our parent.
        var count = jdbc.update(DECREMENT_DEPEND, finishedTask.taskId)
        if (finishedTask.parentTaskId != null) {
            count += jdbc.update(DECREMENT_DEPEND, finishedTask.parentTaskId)
        }
        return count
    }

    override fun incrementDependCount(task: TaskId): Int {
        var count = jdbc.update(INCREMENT_DEPEND, task.taskId)
        if (task.parentTaskId != null) {
            count += jdbc.update(INCREMENT_DEPEND, task.parentTaskId)
        }
        return count
    }

    override fun createParentDepend(child: TaskId): Boolean {
        // Might have to check if the parent task is done.
        return jdbc.update(SET_DEPEND, child.parentTaskId, child.taskId) > 0
    }

    override fun getWaiting(limit: Int): List<TaskStartT> {
        return jdbc.query<TaskStartT>(GET_WAITING, EXECUTE_TASK_MAPPER, limit)
    }

    override fun getOrphanTasks(limit: Int, duration: Long, unit: TimeUnit): List<Task> {
        return jdbc.query<Task>(GET_QUEUED, MAPPER,
                System.currentTimeMillis() - unit.toMillis(duration), limit)
    }

    override fun getAll(job: Int, page: Pager): PagedList<Task> {
        return PagedList(page.setTotalCount(countByJob(job)),
                jdbc.query<Task>(GET_TASKS + "WHERE task.pk_job=? ORDER BY pk_task LIMIT ? OFFSET ?",
                        MAPPER, job, page.size, page.from))
    }

    override fun getAll(job: Int, pager: Pager, filter: TaskFilter): PagedList<Task> {
        filter.setJobId(job)
        val q = filter.getCountQuery("SELECT COUNT(1) FROM task ")
        val count = jdbc.queryForObject(q, Long::class.java, *filter.getValues())

        return PagedList(pager.setTotalCount(count),
                jdbc.query<Task>(filter.getQuery(GET_TASKS, pager),
                        MAPPER, *filter.getValues(pager)))
    }

    override fun getAll(job: Int, state: TaskState): List<Task> {
        return jdbc.query<Task>(GET_TASKS + "WHERE task.pk_job=? AND task.int_state=?",
                MAPPER, job, state.ordinal)
    }

    override fun getAll(job: Int, filter: TaskFilter): List<Task> {
        filter.setJobId(job)

        return jdbc.query<Task>(filter.getQuery(GET_TASKS, null),
                MAPPER, *filter.getValues())
    }

    override fun get(id: Int): Task {
        return jdbc.queryForObject<Task>(GET_TASKS + "WHERE task.pk_task=?", MAPPER, id)
    }

    override fun countByJob(job: Int): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE task.pk_job=?", Long::class.java, job)
    }

    override fun updatePingTime(taskIds: List<Int>): Int {
        if (taskIds.isEmpty()) {
            return 0
        }

        Collections.sort(taskIds)
        val time = System.currentTimeMillis()
        return jdbc.batchUpdate(UPDATE_PING, object : BatchPreparedStatementSetter {
            @Throws(SQLException::class)
            override fun setValues(ps: PreparedStatement, i: Int) {
                ps.setLong(1, time)
                ps.setInt(2, taskIds[i])
            }

            override fun getBatchSize(): Int {
                return taskIds.size
            }
        }).size
    }

    companion object {

        private val INSERT = JdbcUtils.insert("task",
                "pk_job",
                "pk_parent",
                "str_name",
                "int_state",
                "int_order",
                "time_created",
                "time_state_change",
                "time_ping")

        private val INC_STATS = "UPDATE " +
                "task_stat " +
                "SET " +
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_update_count=int_asset_update_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?," +
                "int_asset_replace_count=int_asset_replace_count+? " +
                "WHERE " +
                "pk_task=?"

        private val CLEAR_STATS = "UPDATE " +
                "task_stat " +
                "SET " +
                "int_asset_create_count=0," +
                "int_asset_update_count=0," +
                "int_asset_warning_count=0," +
                "int_asset_error_count=0," +
                "int_asset_replace_count=0 " +
                "WHERE " +
                "pk_task=?"

        private val DECREMENT_DEPEND = "UPDATE " +
                "task " +
                "SET " +
                "int_depend_count=int_depend_count-1 " +
                "WHERE " +
                "pk_depend_parent = ? " +
                "AND " +
                "int_depend_count > 0"

        private val INCREMENT_DEPEND = "UPDATE " +
                "task " +
                "SET " +
                "int_depend_count=int_depend_count+1 " +
                "WHERE " +
                "pk_depend_parent=?"

        private val SET_DEPEND = "UPDATE " +
                "task " +
                "SET " +
                "pk_parent=null," +
                "int_depend_count=1, " +
                "pk_depend_parent=? " +
                "WHERE " +
                "pk_task=? " +
                "AND " +
                "pk_depend_parent IS NULL"

        private val GET_TASK_TO_EXECUTE = "SELECT " +
                "task.pk_task," +
                "task.pk_job, " +
                "task.pk_parent, " +
                "task.int_order," +
                "job.json_args," +
                "job.json_env, " +
                "job.str_root_path, " +
                "task.str_name, " +
                "u.str_username," +
                "u.hmac_key " +
                "FROM " +
                "task," +
                "job,  " +
                "users u " +
                "WHERE " +
                "task.pk_job = job.pk_job " +
                "AND " +
                "job.int_user_created = u.pk_user "

        private val GET_WAITING = GET_TASK_TO_EXECUTE +
                "AND " +
                "job.int_state = 0 " +
                "AND " +
                "task.int_state = 0 " +
                "AND " +
                "task.int_depend_count = 0 " +
                "ORDER BY " +
                "task.int_order ASC, " +
                "task.pk_task ASC " +
                "LIMIT ? "

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
                "task_stat.int_asset_update_count," +
                "job.str_root_path " +

                "FROM " +
                "task " +
                "JOIN task_stat ON task.pk_task = task_stat.pk_task " +
                "JOIN job ON task.pk_job = job.pk_job "


        private val GET_QUEUED = GET_TASKS +
                "WHERE " +
                "task.int_state IN (" + TaskState.Running.ordinal + "," + TaskState.Queued.ordinal + ") " +
                "AND " +
                "task.time_ping < ? " +
                "LIMIT ? "

        private val MAPPER = RowMapper<Task> { rs, _ ->
            val task = Task()
            task.taskId = rs.getInt("pk_task")
            task.jobId = rs.getInt("pk_job")
            task.parentId = rs.getInt("pk_parent")
            task.name = rs.getString("str_name")
            task.exitStatus = rs.getInt("int_exit_status")
            task.host = rs.getString("str_host")
            task.state = TaskState.values()[rs.getInt("int_state")]
            task.timeCreated = rs.getLong("time_created")
            task.timeStarted = rs.getLong("time_started")
            task.timeStopped = rs.getLong("time_stopped")
            task.timeStateChange = rs.getLong("time_state_change")
            task.order = rs.getInt("int_order")

            val workDir = rs.getString("str_root_path")
            task.scriptPath = scriptPath(workDir, task.name, task.id)
            task.logPath = logPath(workDir, task.name, task.id)

            val s = AssetStats()
            s.assetTotalCount = rs.getInt("int_asset_total_count")
            s.assetCreatedCount = rs.getInt("int_asset_create_count")
            s.assetReplacedCount = rs.getInt("int_asset_replace_count")
            s.assetErrorCount = rs.getInt("int_asset_error_count")
            s.assetWarningCount = rs.getInt("int_asset_warning_count")
            s.assetUpdatedCount = rs.getInt("int_asset_update_count")
            task.stats = s
            task
        }

        private val UPDATE_PING = "UPDATE task SET time_ping=? WHERE pk_task=?"

        fun scriptPath(root: String, name: String, id: Int): String {
            return String.format("%s/scripts/%s.%04d.json",
                    root, name.replace("[\\s\\.\\/\\\\]+".toRegex(), "_"), id)
        }

        fun logPath(root: String, name: String, id: Int): String {
            return String.format("%s/logs/%s.%04d.log",
                    root, name.replace("[\\s\\.\\/\\\\]+".toRegex(), "_"), id)
        }
    }
}

