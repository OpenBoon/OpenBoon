package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.MeterRegistryHolder
import com.zorroa.archivist.service.event
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils.insert
import com.zorroa.common.util.Json
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.TimeUnit

interface JobDao {
    fun create(spec: JobSpec, type: PipelineType): Job
    fun update(job: JobId, update: JobUpdateSpec): Boolean
    fun get(id: UUID, forClient:Boolean=false): Job
    fun setState(job: JobId, newState: JobState, oldState: JobState?): Boolean
    fun getAll(filt: JobFilter?): KPagedList<Job>
    fun incrementAssetStats(job: JobId, counts: BatchCreateAssetsResponse) : Boolean
    fun setTimeStarted(job: JobId): Boolean
    fun getExpired(duration: Long, unit: TimeUnit, limit: Int) : List<Job>
    fun delete(job: JobId): Boolean
    fun hasPendingFrames(job: JobId) : Boolean
    fun resumePausedJobs() : Int
}

@Repository
class JobDaoImpl : AbstractDao(), JobDao {

    @Autowired
    lateinit var userDaoCache: UserDaoCache

    override fun create(spec: JobSpec, type: PipelineType): Job {
        Preconditions.checkNotNull(spec.name)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        val user = getUser()

        val pauseUntil = if (spec.pauseDurationSeconds == null) {
            -1
        }
        else {
            spec.paused = true
            time + (spec.pauseDurationSeconds * 1000L)
        }

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, user.organizationId)
            ps.setString(3, spec.name)
            ps.setInt(4, JobState.Active.ordinal)
            ps.setInt(5, type.ordinal)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setLong(8, -1)
            ps.setObject(9, user.id)
            ps.setObject(10, user.id)
            ps.setString(11, Json.serializeToString(spec.args, "{}"))
            ps.setString(12, Json.serializeToString(spec.env, "{}"))
            ps.setInt(13, spec.priority)
            ps.setBoolean(14, spec.paused)
            ps.setLong(15, pauseUntil)
            ps
        }

        jdbc.update("INSERT INTO job_count (pk_job, time_updated) VALUES (?, ?)", id, time)
        jdbc.update("INSERT INTO job_stat (pk_job) VALUES (?)", id)

        logger.event(LogObject.JOB, LogAction.CREATE, mapOf("jobId" to id, "jobName" to spec.name))

        return get(id)
    }

    override fun update(job: JobId, update: JobUpdateSpec): Boolean {
        return jdbc.update(UPDATE,
                update.name, update.priority, update.paused, update.timePauseExpired, job.jobId) == 1
    }

    override fun delete(job: JobId): Boolean {
        val result = listOf(
                "DELETE FROM export_file WHERE pk_job=?",
                "DELETE FROM task_stat WHERE pk_job=?",
                "DELETE FROM task_error WHERE pk_job=?",
                "DELETE FROM task WHERE pk_job=?",
                "DELETE FROM job_count WHERE pk_job=?",
                "DELETE FROM job_stat WHERE pk_job=?",
                "DELETE FROM job WHERE pk_job=?"
        ).map { jdbc.update(it, job.jobId) }
        return result.last() == 1
    }

    override fun get(id: UUID, forClient: Boolean): Job {
        return if (forClient) {
            jdbc.queryForObject("$GET WHERE job.pk_job=?", MAPPER_FOR_CLIENT, id)
        } else {
            jdbc.queryForObject("$GET WHERE job.pk_job=?", MAPPER, id)
        }
    }

    override fun getAll(filt: JobFilter?): KPagedList<Job> {
        val filter = filt ?: JobFilter()
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER_FOR_CLIENT, *values))
    }

    override fun setTimeStarted(job: JobId): Boolean {
        return jdbc.update("UPDATE job SET time_started=? WHERE pk_job=? AND time_started=-1",
                System.currentTimeMillis(), job.jobId) == 1
    }

    override fun getExpired(duration: Long, unit: TimeUnit, limit: Int) : List<Job> {
        val cutOff = System.currentTimeMillis() - unit.toMillis(duration)
        return jdbc.query("$GET_EXPIRED LIMIT ?", MAPPER,
                JobState.Cancelled.ordinal, JobState.Finished.ordinal, cutOff, limit)
    }

    override fun setState(job: JobId, newState: JobState, oldState: JobState?): Boolean {
        val time = System.currentTimeMillis()
        val result =  if (oldState != null) {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=? AND int_state=?",
                    newState.ordinal, time, job.jobId, oldState.ordinal) == 1
        } else {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=?",
                    newState.ordinal, time, job.jobId) == 1
        }
        if (result) {
            meterRegistry.counter("zorroa.job.state",
                    MeterRegistryHolder.getTags(newState.metricsTag())).increment()
            logger.event(LogObject.JOB, LogAction.STATE_CHANGE,
                    mapOf("jobId" to job.jobId,
                            "newState" to newState.name,
                            "oldState" to oldState?.name,
                            "status" to result))
        }
        return result

    }

    override fun hasPendingFrames(job: JobId) : Boolean {
        return jdbc.queryForObject(HAS_PENDING, Int::class.java, JobState.Active.ordinal, job.jobId) == 1
    }

    override fun incrementAssetStats(job: JobId, counts: BatchCreateAssetsResponse) : Boolean {
        return jdbc.update(INC_STATS,
                counts.total,
                counts.createdAssetIds.size,
                counts.warningAssetIds.size,
                counts.erroredAssetIds.size,
                counts.replacedAssetIds.size,
                job.jobId) == 1
    }

    override fun resumePausedJobs() : Int {
        val time = System.currentTimeMillis()
        return jdbc.update(RESUME_PAUSED, JobState.Active.ordinal, time)
    }

    private fun count(filter: JobFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    private val MAPPER_FOR_CLIENT = RowMapper { rs, row ->
        val job = MAPPER.mapRow(rs, row)
        job.assetCounts =  buildAssetCounts(rs)
        job.taskCounts = buildTaskCountMap(rs)
        job.createdUser = userDaoCache.getUser(rs.getObject("pk_user_created") as UUID)
        job
    }

    companion object {

        private inline fun getTaskStateCount(rs: ResultSet, state: TaskState) : Int {
            return rs.getInt("int_task_state_${state.ordinal}")
        }

        private inline fun buildTaskCountMap(rs: ResultSet) : Map<String, Int> {
            val result = mutableMapOf("tasksTotal" to rs.getInt("int_task_total_count"))
            return TaskState.values().map {
                "tasks" + it.toString() to getTaskStateCount(rs, it)
            }.toMap(result)
            return result
        }

        private inline fun buildAssetCounts(rs: ResultSet) : Map<String, Int> {
            val result = mutableMapOf<String,Int>()
            result["assetCreatedCount"] = rs.getInt("int_asset_create_count")
            result["assetReplacedCount"] = rs.getInt("int_asset_replace_count")
            result["assetWarningCount"] = rs.getInt("int_asset_warning_count")
            result["assetErrorCount"] = rs.getInt("int_asset_error_count")
            result["assetTotalCount"] = rs.getInt("int_asset_total_count")
            return result
        }

        private val MAPPER = RowMapper { rs, _ ->
            val state =  JobState.values()[rs.getInt("int_state")]
            Job(rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    PipelineType.values()[rs.getInt("int_type")],
                    state,
                    null,
                    null,
                    null,
                    rs.getLong("time_started"),
                    rs.getLong("time_modified"),
                    rs.getLong("time_created"),
                    rs.getInt("int_priority"),
                    rs.getBoolean("bool_paused"),
                    rs.getLong("time_pause_expired")
            )
        }

        private const val GET = "SELECT * FROM job " +
                "INNER JOIN job_stat ON job.pk_job=job_stat.pk_job " +
                "INNER JOIN job_count ON job.pk_job=job_count.pk_job "

        private const val COUNT = "SELECT COUNT(1) FROM job"

        private const val GET_EXPIRED = "$GET " +
                "WHERE " +
                "job.pk_job = job_count.pk_job " +
                "AND " +
                "job.int_state IN (?,?) " +
                "AND " +
                "job_count.time_updated < ? "

        private const val INC_STATS = "UPDATE " +
                "job_stat " +
                "SET " +
                "int_asset_total_count=int_asset_total_count+?," +
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?," +
                "int_asset_replace_count=int_asset_replace_count+? " +
                "WHERE " +
                "pk_job=?"

        private const val RESUME_PAUSED =
                "UPDATE " +
                    "job " +
                "SET " +
                    "bool_paused='f' " +
                "WHERE " +
                    "int_state=? " +
                "AND " +
                    "bool_paused='t' " +
                "AND " +
                    "time_pause_expired < ? " +
                "AND " +
                    "time_pause_expired != -1"

        private const val UPDATE = "UPDATE " +
                "job " +
            "SET " +
                "str_name=?, int_priority=?, bool_paused=?, time_pause_expired=? " +
            "WHERE pk_job=?"

        private val INSERT = insert("job",
                "pk_job",
                "pk_organization",
                "str_name",
                "int_state",
                "int_type",
                "time_created",
                "time_modified",
                "time_started",
                "pk_user_created",
                "pk_user_modified",
                "json_args",
                "json_env",
                "int_priority",
                "bool_paused",
                "time_pause_expired")

        private const val HAS_PENDING = "SELECT " +
                "COUNT(1) " +
                "FROM " +
                    "job, job_count " +
                "WHERE " +
                    "job.pk_job = job_count.pk_job " +
                "AND " +
                    "job_count.int_task_state_4 + job_count.int_task_state_2 != job_count.int_task_total_count " +
                "AND " +
                    "job.int_state = ? " +
                "AND " +
                    "job.pk_job = ?"
    }
}

