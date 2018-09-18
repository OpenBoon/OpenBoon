package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import com.zorroa.common.util.JdbcUtils.insert
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface JobDao {
    fun create(spec: JobSpec, type: PipelineType): Job
    fun get(id: UUID): Job
    fun setState(job: Job, newState: JobState, oldState: JobState?): Boolean
    fun getAll(pager: Pager, filter: JobFilter?): PagedList<Job>
}

@Repository
class JobDaoImpl : AbstractDao(), JobDao {

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
        return get(id)
    }

    override fun get(id: UUID): Job {
        return jdbc.queryForObject("$GET WHERE pk_job=?", MAPPER, id)
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

    private fun count(filter: JobFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
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
                "time_started",
                "pk_user_created",
                "pk_user_modified",
                "json_args",
                "json_env")
    }
}

