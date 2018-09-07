package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.*
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils.insert
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface JobDao {
    fun create(spec: JobSpec): Job
    fun get(id: UUID) : Job
    fun setState(job: Job, newState: JobState, oldState: JobState?) : Boolean
    fun getAll(filter: JobFilter) : KPagedList<Job>
}

@Repository
class JobDaoImpl : AbstractDao(), JobDao {

    override fun create(spec: JobSpec): Job {
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
            ps.setInt(5, spec.type.ordinal)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setObject(8, user.id)
            ps.setObject(9, user.id)
            ps.setString(10, Json.serializeToString(spec.args, "{}"))
            ps.setString(11, Json.serializeToString(spec.env, "{}"))
            ps
        }
        return get(id)
    }

    override fun get(id: UUID) : Job {
        return jdbc.queryForObject("$GET WHERE pk_job=?", MAPPER, id)
    }

    override fun getAll(filter: JobFilter) : KPagedList<Job> {
        setCount(filter)
        return KPagedList(filter.page, jdbc.query(filter.getQuery(GET),
                        MAPPER, *filter.getValues()))
    }

    override fun setState(job: Job, newState: JobState, oldState: JobState?) : Boolean {
        val time = System.currentTimeMillis()
        return if (oldState != null) {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=? AND int_state=?",
                    newState.ordinal, time, job.id, oldState.ordinal) == 1
        }
        else {
            jdbc.update("UPDATE job SET int_state=?,time_modified=? WHERE pk_job=?",
                    newState.ordinal, time, job.id) == 1
        }
    }

    private fun setCount(filter: JobFilter) {
        filter?.page?.totalCount = jdbc.queryForObject(filter.getCountQuery(COUNT),
                Long::class.java, *filter.getValues(forCount = true))
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Job(rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    PipelineType.values()[rs.getInt("int_type")],
                    JobState.values()[rs.getInt("int_state")])
        }

        private const val GET = "SELECT * FROM job"

        private const val COUNT = "SELECT COUNT(1) FROM job"

        private val INSERT = insert("job",
                "pk_job",
                    "pk_organization",
                    "str_name",
                    "int_state",
                    "int_type",
                    "time_created",
                    "time_modified",
                    "user_created",
                    "user_modified",
                    "json_args",
                    "json_env")
    }
}

