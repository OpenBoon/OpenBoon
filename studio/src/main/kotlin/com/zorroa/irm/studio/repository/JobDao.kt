package com.zorroa.irm.studio.repository

import com.google.common.base.Preconditions
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface JobDao {
    fun create(spec: JobSpec): Job
    fun get(id: UUID) : Job
    fun get(name: String) : Job
    fun setState(job: Job, newState: JobState, oldState: JobState?) : Boolean
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
            ps.setObject(2, spec.assetId)
            ps.setObject(3, spec.organizationId)
            ps.setString(4, spec.name)
            ps.setInt(5, JobState.WAITING.ordinal)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setArray(8, connection.createArrayOf("text", spec.pipelines.toTypedArray()))
            ps.setString(9, Json.serializeToString(spec.attrs, "{}"))
            ps.setString(10, Json.serializeToString(spec.env, "{}"))
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

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Job(rs.getObject("pk_job") as UUID,
                    rs.getObject("pk_asset") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"),
                    JobState.values()[rs.getInt("int_state")],
                    (rs.getArray("list_pipelines").array as Array<String>).toList(),
                    Json.deserialize(rs.getString("json_attrs"), Json.GENERIC_MAP),
                    Json.deserialize(rs.getString("json_env"), Json.STRING_MAP))
        }

        private const val GET = "SELECT * FROM job"

        private val INSERT = sqlInsert("job",
                "pk_job",
                    "pk_asset",
                    "pk_organization",
                    "str_name",
                    "int_state",
                    "time_created",
                    "time_modified",
                    "list_pipelines",
                    "json_attrs",
                    "json_env")
    }
}

