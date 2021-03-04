package boonai.archivist.repository

import boonai.archivist.domain.AssetCounters
import boonai.archivist.domain.Credentials
import boonai.archivist.domain.CredentialsType
import boonai.archivist.domain.Job
import boonai.archivist.domain.JobFilter
import boonai.archivist.domain.JobId
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.JobState
import boonai.archivist.domain.JobUpdateSpec
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.TaskStateCounts
import boonai.archivist.security.getZmlpActor
import boonai.archivist.security.getZmlpActorOrNull
import boonai.archivist.util.JdbcUtils.getTsWordVector
import boonai.common.service.logging.event
import boonai.archivist.util.JdbcUtils.insert
import boonai.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID
import java.util.concurrent.TimeUnit

interface JobDao {
    fun create(spec: JobSpec): Job
    fun update(job: JobId, update: JobUpdateSpec): Boolean
    fun get(id: UUID, forClient: Boolean = false): Job
    fun setState(job: JobId, newState: JobState, oldState: JobState?): Boolean
    fun getAll(filt: JobFilter?): KPagedList<Job>
    fun incrementAssetCounters(job: JobId, counts: AssetCounters): Boolean
    fun setTimeStarted(job: JobId): Boolean
    fun getExpired(duration: Long, unit: TimeUnit, limit: Int): List<Job>
    fun delete(job: JobId): Boolean
    fun resumePausedJobs(): Int
    fun findOneJob(filter: JobFilter): Job
    fun getTaskStateCounts(job: JobId): TaskStateCounts
    fun setCredentials(job: JobId, creds: List<Credentials>)
    fun getCredentialsTypes(job: JobId): List<String>
}

@Repository
class JobDaoImpl : AbstractDao(), JobDao {

    override fun create(spec: JobSpec): Job {
        if (spec.name == null) {
            throw IllegalArgumentException("The job name cannot be null.")
        }

        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        val key = getZmlpActor()

        val pauseUntil = if (spec.pauseDurationSeconds == null) {
            -1
        } else {
            spec.paused = true
            time + (spec.pauseDurationSeconds * 1000L)
        }

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, key.projectId)
            ps.setObject(3, spec.dataSourceId)
            ps.setString(4, spec.name)
            ps.setInt(5, JobState.InProgress.ordinal)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setLong(8, -1)
            ps.setLong(9, -1)
            ps.setString(10, Json.serializeToString(spec.args, "{}"))
            ps.setString(11, Json.serializeToString(spec.env, "{}"))
            ps.setInt(12, spec.priority)
            ps.setBoolean(13, spec.paused)
            ps.setLong(14, pauseUntil)
            ps.setObject(15, getTsWordVector(spec.name))
            ps
        }

        jdbc.update(
            "INSERT INTO job_count (pk_job, time_updated, int_max_running_tasks) VALUES (?, ?, ?)",
            id, time, spec.maxRunningTasks
        )
        jdbc.update("INSERT INTO job_stat (pk_job) VALUES (?)", id)

        logger.event(LogObject.JOB, LogAction.CREATE, mapOf("jobId" to id, "jobName" to spec.name))

        return get(id)
    }

    override fun update(job: JobId, update: JobUpdateSpec): Boolean {
        jdbc.update(
            "UPDATE job_count SET int_max_running_tasks=? WHERE pk_job=?",
            update.maxRunningTasks, job.jobId
        )
        return jdbc.update(
            UPDATE,
            update.name, update.priority, update.paused, update.timePauseExpired, getTsWordVector(update.name), job.jobId,
        ) == 1
    }

    override fun delete(job: JobId): Boolean {
        val result = listOf(
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
            val pid = getZmlpActorOrNull()
            if (pid != null) {
                jdbc.queryForObject("$GET WHERE job.pk_job=? AND job.pk_project=?", MAPPER, id, pid.projectId)
            } else {
                jdbc.queryForObject("$GET WHERE job.pk_job=?", MAPPER, id)
            }
        }
    }

    override fun getAll(filt: JobFilter?): KPagedList<Job> {
        val filter = filt ?: JobFilter()
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER_FOR_CLIENT, *values))
    }

    override fun findOneJob(filter: JobFilter): Job {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject<Job>(query, MAPPER, *values)
    }

    override fun setTimeStarted(job: JobId): Boolean {
        return jdbc.update(
            "UPDATE job SET time_started=? WHERE pk_job=? AND time_started=-1",
            System.currentTimeMillis(), job.jobId
        ) == 1
    }

    override fun getExpired(duration: Long, unit: TimeUnit, limit: Int): List<Job> {
        val cutOff = System.currentTimeMillis() - unit.toMillis(duration)
        return jdbc.query(
            "$GET_EXPIRED LIMIT ?", MAPPER,
            JobState.Cancelled.ordinal, JobState.Success.ordinal, JobState.Failure.ordinal, cutOff, limit
        )
    }

    override fun setState(job: JobId, newState: JobState, oldState: JobState?): Boolean {
        val time = System.currentTimeMillis()
        val result = if (oldState != null) {
            jdbc.update(
                "UPDATE job SET int_state=?,time_modified=? WHERE pk_job=? AND int_state=?",
                newState.ordinal, time, job.jobId, oldState.ordinal
            ) == 1
        } else {
            jdbc.update(
                "UPDATE job SET int_state=?,time_modified=? WHERE pk_job=? AND int_state!=?",
                newState.ordinal, time, job.jobId, newState.ordinal
            ) == 1
        }
        if (result) {

            if (newState.isInactiveState()) {
                jdbc.update("UPDATE job SET time_stopped=? WHERE pk_job=?", time, job.jobId)
            } else if (newState.isActiveState()) {
                jdbc.update("UPDATE job SET time_stopped=-1 WHERE pk_job=?", job.jobId)
            }

            logger.event(
                LogObject.JOB, LogAction.STATE_CHANGE,
                mapOf(
                    "jobId" to job.jobId,
                    "newState" to newState.name,
                    "oldState" to oldState?.name,
                    "status" to result
                )
            )
        }
        return result
    }

    override fun getTaskStateCounts(job: JobId): TaskStateCounts {
        return jdbc.queryForObject(
            GET_TASK_COUNTS,
            RowMapper { rs, i ->
                val map = mapOf(
                    TaskState.Waiting to rs.getInt("int_task_state_0"),
                    TaskState.Running to rs.getInt("int_task_state_1"),
                    TaskState.Success to rs.getInt("int_task_state_2"),
                    TaskState.Failure to rs.getInt("int_task_state_3"),
                    TaskState.Skipped to rs.getInt("int_task_state_4"),
                    TaskState.Queued to rs.getInt("int_task_state_5"),
                    TaskState.Depend to rs.getInt("int_task_state_6")
                )
                TaskStateCounts(map, rs.getInt("int_task_total_count"))
            },
            job.jobId
        )
    }

    override fun incrementAssetCounters(job: JobId, counts: AssetCounters): Boolean {
        return jdbc.update(
            ASSET_COUNTS_INC,
            counts.total,
            job.jobId
        ) == 1
    }

    override fun setCredentials(job: JobId, creds: List<Credentials>) {
        logger.event(
            LogObject.JOB, LogAction.UPDATE,
            mapOf(
                "jobId" to job.jobId,
                "credentialsId" to creds.map { it.id }
            )
        )
        jdbc.update("DELETE FROM x_credentials_job WHERE pk_job=?", job.jobId)
        creds.forEach {
            jdbc.update(
                "INSERT INTO x_credentials_job VALUES (?,?,?,?)",
                UUID.randomUUID(), it.id, job.jobId, it.type.ordinal
            )
        }
    }

    override fun getCredentialsTypes(job: JobId): List<String> {
        return jdbc.queryForList(
            "SELECT int_type FROM x_credentials_job WHERE pk_job=?", Int::class.java,
            job.jobId
        ).map {
            CredentialsType.values()[it].toString()
        }
    }

    override fun resumePausedJobs(): Int {
        val time = System.currentTimeMillis()
        return jdbc.update(RESUME_PAUSED, JobState.InProgress.ordinal, time)
    }

    private fun count(filter: JobFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    private val MAPPER_FOR_CLIENT = RowMapper { rs, row ->
        val job = MAPPER.mapRow(rs, row)
        job.assetCounts = buildAssetCounts(rs)
        job.taskCounts = buildTaskCountMap(rs)
        job
    }

    companion object {

        private inline fun getTaskStateCount(rs: ResultSet, state: TaskState): Int {
            return rs.getInt("int_task_state_${state.ordinal}")
        }

        private inline fun buildTaskCountMap(rs: ResultSet): Map<String, Int> {
            val result = mutableMapOf("tasksTotal" to rs.getInt("int_task_total_count"))
            return TaskState.values().map {
                "tasks$it" to getTaskStateCount(rs, it)
            }.toMap(result)
            return result
        }

        private inline fun buildAssetCounts(rs: ResultSet): Map<String, Int> {
            val result = mutableMapOf<String, Int>()
            result["assetTotalCount"] = rs.getInt("int_asset_total_count")
            result["assetCreatedCount"] = 0
            result["assetReplacedCount"] = 0
            result["assetWarningCount"] = rs.getInt("int_asset_warning_count")
            result["assetErrorCount"] = rs.getInt("int_asset_error_count")
            return result
        }

        private val MAPPER = RowMapper { rs, _ ->
            val state = JobState.values()[rs.getInt("int_state")]
            Job(
                rs.getObject("pk_job") as UUID,
                rs.getObject("pk_project") as UUID,
                rs.getObject("pk_datasource") as UUID?,
                rs.getString("str_name"),
                state,
                null,
                null,
                rs.getLong("time_started"),
                rs.getLong("time_modified"),
                rs.getLong("time_created"),
                rs.getLong("time_stopped"),
                rs.getInt("int_priority"),
                rs.getBoolean("bool_paused"),
                rs.getLong("time_pause_expired"),
                rs.getInt("int_max_running_tasks")
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
            "job.int_state IN (?,?,?) " +
            "AND " +
            "job_count.time_updated < ? "

        private const val ASSET_COUNTS_INC = "UPDATE " +
            "job_stat " +
            "SET " +
            "int_asset_total_count=int_asset_total_count+? " +
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
            "str_name=?, int_priority=?, bool_paused=?, time_pause_expired=?, fti_keywords=to_tsvector(?) " +
            "WHERE pk_job=?"

        private val INSERT = insert(
            "job",
            "pk_job",
            "pk_project",
            "pk_datasource",
            "str_name",
            "int_state",
            "time_created",
            "time_modified",
            "time_started",
            "time_stopped",
            "json_args",
            "json_env",
            "int_priority",
            "bool_paused",
            "time_pause_expired",
            "fti_keywords@to_tsvector"
        )

        private const val GET_TASK_COUNTS =
            "SELECT " +
                "job_count.int_task_state_0," +
                "job_count.int_task_state_1," +
                "job_count.int_task_state_2," +
                "job_count.int_task_state_3," +
                "job_count.int_task_state_4," +
                "job_count.int_task_state_5, " +
                "job_count.int_task_state_6, " +
                "job_count.int_task_total_count " +
                "FROM " +
                "job_count " +
                "WHERE " +
                "job_count.pk_job = ?"
    }
}
