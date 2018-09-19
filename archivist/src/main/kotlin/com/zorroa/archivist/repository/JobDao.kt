package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.*
import com.zorroa.common.util.JdbcUtils.insert
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

interface JobDao {
    fun create(spec: JobSpec, type: PipelineType): Job
    fun get(id: UUID): Job
    fun setState(job: Job, newState: JobState, oldState: JobState?): Boolean
    fun getAll(pager: Pager, filter: JobFilter?): PagedList<Job>
    fun incrementAssetStats(job: JobId, counts: AssetIndexResult) : Boolean
    fun getForClient(id: UUID): Job
}

@Repository
class JobDaoImpl : AbstractDao(), JobDao {

    @Autowired
    internal lateinit var userDaoCache: UserDaoCache

    private val FOR_CLIENT_MAPPER = RowMapper { rs, _ ->
        Job(rs.getObject("pk_job") as UUID,
                rs.getObject("pk_organization") as UUID,
                rs.getString("str_name"),
                PipelineType.values()[rs.getInt("int_type")],
                JobState.values()[rs.getInt("int_state")],
                buildAssetCounts(rs),
                buildTaskCountMap(rs),
                userDaoCache.getUser(rs.getObject("pk_user_created") as UUID))
    }

    override fun create(spec: JobSpec, type: PipelineType): Job {
        Preconditions.checkNotNull(spec.name)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(JobDaoImpl.INSERT)
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
            ps
        }
        jdbc.update("INSERT INTO job_count (pk_job) VALUES (?)", id)
        jdbc.update("INSERT INTO job_stat (pk_job) VALUES (?)", id)
        return get(id)
    }

    override fun get(id: UUID): Job {
        return jdbc.queryForObject("$GET WHERE job.pk_job=?", MAPPER, id)
    }

    override fun getForClient(id: UUID): Job {
        return jdbc.queryForObject("$GET WHERE job.pk_job=?", FOR_CLIENT_MAPPER, id)
    }

    override fun getAll(page: Pager, filter: JobFilter?): PagedList<Job> {
        var filter = filter
        if (filter == null) {
            filter = JobFilter()
        }

        val query = filter.getQuery(GET, false)
        return PagedList(page.setTotalCount(count(filter)),
                jdbc.query<Job>(query, MAPPER, *filter.getValues(false)))
    }


    override fun setState(job: Job, newState: JobState, oldState: JobState?): Boolean {
        val time = System.currentTimeMillis()
        return if (oldState != null) {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=? AND int_state=?",
                    newState.ordinal, time, job.id, oldState.ordinal) == 1
        } else {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=?",
                    newState.ordinal, time, job.id) == 1
        }
    }

    override fun incrementAssetStats(job: JobId, counts: AssetIndexResult) : Boolean {
        return jdbc.update(INC_STATS,
                counts.total, counts.created, counts.updated, counts.warnings, counts.errors, counts.replaced, job.jobId) == 1
    }

    private fun count(filter: JobFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    private fun setCount(filter: JobFilter) {
        filter?.page?.totalCount = jdbc.queryForObject(filter.getCountQuery(COUNT),
                Long::class.java, *filter.getValues(forCount = true))
    }
    
    companion object {

        private inline fun buildTaskCountMap(rs: ResultSet) : Map<String, Int> {
            val result = mutableMapOf<String,Int>()
            for (state in TaskState.values()) {
                val field = "tasks" + state.toString()
                val ord = state.ordinal
                result[field] =  rs.getInt("int_task_state_$ord")
            }
            result["tasksCompleted"] = rs.getInt("int_task_state_2")
                    + rs.getInt("int_task_state_3")
                    + rs.getInt("int_task_state_4")
            return result
        }

        private inline fun buildAssetCounts(rs: ResultSet) : Map<String, Int> {
            val result = mutableMapOf<String,Int>()
            result["assetCreatedCount"] = rs.getInt("int_asset_create_count")
            result["assetReplacedCount"] = rs.getInt("int_asset_replace_count")
            result["assetWarningCount"] = rs.getInt("int_asset_warning_count")
            result["assetErrorCount"] = rs.getInt("int_asset_error_count")
            result["assetUpdateCount"] = rs.getInt("int_asset_update_count")
            result["assetTotalCount"] = rs.getInt("int_asset_total_count")
            return result
        }

        private val MAPPER = RowMapper { rs, _ ->
            Job(rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    PipelineType.values()[rs.getInt("int_type")],
                    JobState.values()[rs.getInt("int_state")],
                    buildAssetCounts(rs),
                    buildTaskCountMap(rs))
        }

        private const val GET = "SELECT * FROM job " +
                "INNER JOIN job_stat ON job.pk_job=job_stat.pk_job " +
                "INNER JOIN job_count ON job.pk_job=job_count.pk_job "

        private const val COUNT = "SELECT COUNT(1) FROM job"


        private const val INC_STATS = "UPDATE " +
                "job_stat " +
                "SET " +
                "int_asset_total_count=int_asset_total_count+?," +
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_update_count=int_asset_update_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?," +
                "int_asset_replace_count=int_asset_replace_count+? " +
                "WHERE " +
                "pk_job=?"


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
                "json_env")
    }
}

