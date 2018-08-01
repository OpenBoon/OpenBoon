package com.zorroa.analyst.repository

import com.google.common.base.Preconditions
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.springframework.jdbc.`object`.BatchSqlUpdate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Types
import java.util.*

interface JobDao {
    fun create(spec: JobSpec): Job
    fun get(id: UUID) : Job
    fun get(name: String) : Job
    fun setState(job: Job, newState: JobState, oldState: JobState?) : Boolean
    fun getWaiting(count:Int) : List<Job>
    fun getRunning() : List<Job>
    fun getAll(filter: JobFilter) : KPagedList<Job>
    fun setCount(filter: JobFilter)
    fun mapAssetsToJob(job: Job, assets: List<UUID>)

}

@Repository
class JobDaoImpl : AbstractJdbcDao(), JobDao {

    override fun create(spec: JobSpec): Job {
        Preconditions.checkNotNull(spec.name)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(JobDaoImpl.INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.organizationId)
            ps.setString(3, spec.name)
            ps.setInt(4, JobState.SETUP.ordinal)
            ps.setInt(5, spec.type.ordinal)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setString(8, Json.serializeToString(spec.attrs, "{}"))
            ps.setString(9, Json.serializeToString(spec.env, "{}"))
            ps.setBoolean(10, spec.lockAssets)
            ps
        })
        return get(id)
    }

    override fun get(id: UUID) : Job {
        return jdbc.queryForObject("$GET WHERE pk_job=?", MAPPER, id)
    }

    override fun get(name: String) : Job {
        return jdbc.queryForObject("$GET WHERE str_name=?", MAPPER, name)
    }

    override fun getWaiting(count : Int) : List<Job> {
        return jdbc.query(GET_WAITING , MAPPER, JobState.WAITING.ordinal, count)
    }

    override fun getRunning() : List<Job> {
        return jdbc.query(GET_RUNNING, MAPPER, JobState.RUNNING.ordinal)
    }

    override fun getAll(filter: JobFilter) : KPagedList<Job> {
        setCount(filter)
        return KPagedList(filter.page, jdbc.query(filter.getQuery(GET),
                        MAPPER, *filter.getValues()))
    }

    override fun setCount(filter: JobFilter) {
        filter?.page?.totalCount = jdbc.queryForObject(filter.getCountQuery(COUNT),
                Long::class.java, *filter.getValues(forCount = true))
    }

    override fun setState(job: Job, newState: JobState, oldState: JobState?) : Boolean {
        val time = System.currentTimeMillis()
        val updated =  if (oldState != null) {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=? AND int_state=?",
                    newState.ordinal, time, job.id, oldState.ordinal) == 1
        }
        else {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=?",
                    newState.ordinal, time, job.id) == 1
        }
        if (updated) {
            if (newState in START_STATES) {
                jdbc.update("UPDATE job SET time_started=?, time_stopped=-1 WHERE pk_job=?", time, job.id)
            }
            else if (newState in STOP_STATES) {
                jdbc.update("UPDATE job SET time_stopped=? WHERE pk_job=?", time, job.id)
            }
        }
        return updated
    }

    override fun mapAssetsToJob(job: Job, assets: List<UUID>) {
        val batch = BatchSqlUpdate(jdbc.dataSource,
                "INSERT INTO x_asset_job VALUES (?,?,?)",
                intArrayOf(Types.OTHER, Types.OTHER, Types.OTHER), 50)
        for (asset in assets) {
            batch.update(uuid1.generate(), job.id, asset)
        }
        batch.flush()
    }

    companion object {

        private val START_STATES = setOf(JobState.RUNNING)

        private val STOP_STATES = setOf(JobState.ORPHAN, JobState.SUCCESS, JobState.FAIL)

        private val MAPPER = RowMapper { rs, _ ->
            Job(rs.getObject("pk_job") as UUID,
                    PipelineType.values()[rs.getInt("int_type")],
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    JobState.values()[rs.getInt("int_state")],
                    rs.getBoolean("bool_lock_assets"),
                    Json.deserialize(rs.getString("json_attrs"), Json.GENERIC_MAP),
                    Json.deserialize(rs.getString("json_env"), Json.STRING_MAP))
        }

        private const val GET = "SELECT * FROM job"

        private const val COUNT = "SELECT COUNT(1) FROM job"

        private const val GET_WAITING = "$GET " +
                "WHERE " +
                    "int_state=? " +
                "AND " +
                    "NOT EXISTS (" +
                        "SELECT 1 FROM lock INNER JOIN x_asset_job x ON (x.pk_asset=lock.pk_asset) " +
                        "WHERE x.pk_job = job.pk_job " +
                    ")" +
                "ORDER BY " +
                    "time_created ASC LIMIT ? "

        private const val GET_RUNNING = "$GET " +
                "WHERE " +
                "int_state=? "

        private val INSERT = sqlInsert("job",
                "pk_job",
                    "pk_organization",
                    "str_name",
                    "int_state",
                    "int_type",
                    "time_created",
                    "time_modified",
                    "json_attrs",
                    "json_env",
                    "bool_lock_assets")
    }
}

