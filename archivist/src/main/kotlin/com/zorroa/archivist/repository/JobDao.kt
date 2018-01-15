package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.common.domain.TaskState
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

interface JobDao {

    fun nextId(spec: JobSpec): JobSpec

    fun create(spec: JobSpec): Job

    fun get(id: Int): Job

    fun get(job: JobId): Job

    fun getAll(page: Pager, filter: JobFilter?): PagedList<Job>

    fun count(filter: JobFilter): Long

    fun count(): Long

    fun incrementStats(id: Int, adder: TaskStatsAdder): Boolean

    fun decrementStats(id: Int, stats: AssetStats): Boolean

    fun incrementWaitingTaskCount(job: JobId)

    fun setState(job: JobId, newState: JobState, expect: JobState?): Boolean

    fun getState(id: Int): JobState

    fun getRootPath(id: Int): String

    fun updateTaskStateCounts(task: TaskId, newState: TaskState, expect: TaskState)
}

@Repository
open class JobDaoImpl : AbstractDao(), JobDao {

    @Autowired
    private var userDaoCache: UserDaoCache? = null

    private val MAPPER = RowMapper<Job> { rs, _ ->
        val job = Job()
        job.id = rs.getInt("pk_job")
        job.name = rs.getString("str_name")
        job.timeStarted = rs.getLong("time_started")
        job.timeUpdated = rs.getLong("time_updated")
        job.type = PipelineType.fromObject(rs.getInt("int_type"))
        job.user = userDaoCache!!.getUser(rs.getInt("int_user_created"))
        job.args = Json.deserialize<Map<String, Any>>(rs.getString("json_args"), Json.GENERIC_MAP)
        job.rootPath = rs.getString("str_root_path")

        val a = AssetStats()
        a.assetTotalCount = rs.getInt("int_asset_total_count")
        a.assetCreatedCount = rs.getInt("int_asset_create_count")
        a.assetReplacedCount = rs.getInt("int_asset_replace_count")
        a.assetErrorCount = rs.getInt("int_asset_error_count")
        a.assetWarningCount = rs.getInt("int_asset_warning_count")
        a.assetUpdatedCount = rs.getInt("int_asset_update_count")
        job.stats = a

        val t = Job.Counts()
        t.tasksTotal = rs.getInt("int_task_total_count")
        t.tasksCompleted = rs.getInt("int_task_completed_count")
        t.tasksWaiting = rs.getInt("int_task_state_waiting_count")
        t.tasksQueued = rs.getInt("int_task_state_queued_count")
        t.tasksRunning = rs.getInt("int_task_state_running_count")
        t.tasksSuccess = rs.getInt("int_task_state_success_count")
        t.tasksFailure = rs.getInt("int_task_state_failure_count")
        t.tasksSkipped = rs.getInt("int_task_state_skipped_count")
        job.counts = t

        val state = JobState.values()[rs.getInt("int_state")]
        job.state = state
        job
    }

    override fun create(spec: JobSpec): Job {
        Preconditions.checkNotNull(spec)
        Preconditions.checkNotNull(spec.name)
        Preconditions.checkNotNull(spec.type)
        val time = System.currentTimeMillis()

        nextId(spec)

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setInt(1, spec.jobId!!)
            ps.setString(2, spec.name)
            ps.setInt(3, spec.type.ordinal)
            ps.setInt(4, SecurityUtils.getUser().id)
            ps.setLong(5, time)
            ps.setString(6, Json.serializeToString(spec.args, "{}"))
            ps.setString(7, Json.serializeToString(spec.env, "{}"))
            ps.setString(8, spec.rootPath)
            ps
        }

        // insert supporting tables.
        jdbc.update("INSERT INTO job_stat (pk_job) VALUES (?)", spec.jobId)
        jdbc.update("INSERT INTO job_count (pk_job, time_updated) VALUES (?, ?)", spec.jobId, time)
        return get(spec.jobId!!)
    }

    override fun nextId(spec: JobSpec): JobSpec {
        if (spec.jobId == null) {
            if (isDbVendor("postgresql")) {
                spec.jobId = jdbc.queryForObject("SELECT nextval('zorroa.job_pk_job_seq')", Int::class.java)
            } else {
                spec.jobId = jdbc.queryForObject("SELECT JOB_SEQ.nextval FROM dual", Int::class.java)
            }
        }
        return spec
    }

    override fun get(id: Int): Job {
        return jdbc.queryForObject<Job>(GET + "AND job.pk_job=?", MAPPER, id)
    }

    override fun get(job: JobId): Job {
        return get(job.jobId!!)
    }

    override fun getAll(page: Pager, filter: JobFilter?): PagedList<Job> {
        var filter = filter
        if (filter == null) {
            filter = JobFilter()
        }
        val query = filter.getQuery(GET, page)
        return PagedList(page.setTotalCount(count(filter)),
                jdbc.query<Job>(query.left, MAPPER, *query.right.toTypedArray()))
    }

    override fun count(filter: JobFilter): Long {
        val query = filter.getQuery(COUNT, null)
        return jdbc.queryForObject(query.left, Long::class.java, *query.right.toTypedArray())
    }

    override fun count(): Long {
        return jdbc.queryForObject(COUNT, Long::class.java)
    }

    override fun incrementStats(id: Int, adder: TaskStatsAdder): Boolean {
        return jdbc.update(INC_STATS, adder.create, adder.update,
                adder.warning, adder.error, adder.replace, adder.total, id) == 1
    }

    override fun decrementStats(id: Int, stats: AssetStats): Boolean {
        return jdbc.update(DEC_STATS,
                stats.assetCreatedCount,
                stats.assetUpdatedCount,
                stats.assetWarningCount,
                stats.assetErrorCount,
                stats.assetReplacedCount, id) == 1
    }

    override fun incrementWaitingTaskCount(job: JobId) {
        jdbc.update(INC_WAITING_TASK_COUNT, job.jobId)
    }

    override fun setState(job: JobId, newState: JobState, expect: JobState?): Boolean {
        val values = mutableListOf<Any>()
        val fields = mutableListOf<String>()

        fields.add("int_state=?")
        values.add(newState.ordinal)

        val sb = StringBuilder(256)
        sb.append("UPDATE job SET ")
        sb.append(fields.joinToString(","))
        sb.append(" WHERE pk_job=? ")
        values.add(job.jobId)
        if (expect != null) {
            values.add(expect.ordinal)
            sb.append(" AND int_state=?")
        }
        return jdbc.update(sb.toString(), *values.toTypedArray()) == 1
    }

    override fun getState(id: Int): JobState {
        return if (jdbc.queryForObject("SELECT int_task_total_count - (int_task_state_success_count + int_task_state_skipped_count) AS c FROM job_count WHERE pk_job=?",
                Int::class.java, id) == 0) {
            JobState.Finished
        } else {
            JobState.Active
        }
    }

    override fun getRootPath(id: Int): String {
        return jdbc.queryForObject("SELECT str_root_path FROM job WHERE pk_job=?", String::class.java, id)
    }

    override fun updateTaskStateCounts(task: TaskId, newState: TaskState, expect: TaskState) {
        if (newState == expect) {
            return
        }
        /**
         * TODO: implement as a trigger!
         */
        val p = "int_task_state_" + newState.toString().toLowerCase() + "_count"
        val m = "int_task_state_" + expect.toString().toLowerCase() + "_count"

        val cols = Lists.newArrayList(
                "$p=$p+1",
                "$m=$m-1"
        )


        val update = StringBuilder(256)
                .append("UPDATE job_count SET ")
                .append(cols.joinToString(","))
                .append(" WHERE pk_job=?").toString()

        jdbc.update(update, task.jobId)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("job",
                "pk_job",
                "str_name",
                "int_type",
                "int_user_created",
                "time_started",
                "json_args",
                "json_env",
                "str_root_path")

        private val GET = "SELECT " +
                "job.pk_job," +
                "job.str_name," +
                "job.int_state," +
                "job.time_started," +
                "job.int_type," +
                "job.json_args," +
                "job.int_user_created," +
                "job.str_root_path," +
                "job_stat.int_asset_total_count," +
                "job_stat.int_asset_create_count," +
                "job_stat.int_asset_replace_count," +
                "job_stat.int_asset_error_count," +
                "job_stat.int_asset_warning_count," +
                "job_stat.int_asset_update_count," +
                "job_count.int_task_total_count," +
                "job_count.int_task_completed_count," +
                "job_count.int_task_state_queued_count," +
                "job_count.int_task_state_waiting_count," +
                "job_count.int_task_state_running_count," +
                "job_count.int_task_state_success_count," +
                "job_count.int_task_state_failure_count, " +
                "job_count.int_task_state_skipped_count, " +
                "job_count.time_updated " +
                "FROM " +
                "job," +
                "job_count," +
                "job_stat " +
                "WHERE " +
                "job.pk_job = job_count.pk_job " +
                "AND " +
                "job.pk_job = job_stat.pk_job "

        private val COUNT = "SELECT COUNT(1) FROM job "

        private val INC_STATS = "UPDATE " +
                "job_stat " +
                "SET " +
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_update_count=int_asset_update_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?," +
                "int_asset_replace_count=int_asset_replace_count+?, " +
                "int_asset_total_count=int_asset_total_count+? " +
                "WHERE " +
                "pk_job=?"

        private val DEC_STATS = "UPDATE " +
                "job_stat " +
                "SET " +
                "int_asset_create_count=int_asset_create_count-?," +
                "int_asset_update_count=int_asset_update_count-?," +
                "int_asset_warning_count=int_asset_warning_count-?," +
                "int_asset_error_count=int_asset_error_count-?," +
                "int_asset_replace_count=int_asset_replace_count-? " +
                "WHERE " +
                "pk_job=?"

        private val INC_WAITING_TASK_COUNT = "UPDATE " +
                "job_count " +
                "SET " +
                "int_task_total_count=int_task_total_count+1," +
                "int_task_state_waiting_count=int_task_state_waiting_count+1 " +
                "WHERE " +
                "pk_job=?"
    }
}
