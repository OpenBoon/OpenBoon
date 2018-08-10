package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Export
import com.zorroa.archivist.domain.ExportFilter
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.PipelineType
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface ExportDao {
    fun create(spec: ExportSpec) : Export
    fun get(id: UUID) : Export
    fun getAll(page: KPage, filter: ExportFilter): KPagedList<Export>
    fun count(): Long

    fun setAnalystJobId(export: Export, job: Job) : Boolean
}

@Repository
class ExportDaoImpl : AbstractDao(), ExportDao {

    override fun create(spec: ExportSpec): Export {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, user.organizationId)
            ps.setString(3, spec.name)
            ps.setObject(4, user.id)
            ps.setLong(5, time)
            ps.setString(6, Json.serializeToString(spec.args, "{}"))
            ps.setString(7, Json.serializeToString(spec.env, "{}"))
            ps
        }

        return get(id)
    }

    override fun setAnalystJobId(export: Export, job: Job) : Boolean {
        return jdbc.update("UPDATE export SET job_id=? WHERE pk_export=? AND job_id is NULL",
                job.id, export.id) == 1

    }

    override fun get(id: UUID) : Export {
        return jdbc.queryForObject("$GET WHERE pk_export=? AND pk_organization=?",
                MAPPER, id, getUser().organizationId)
    }

    fun setCount(filter: ExportFilter) {
        filter?.page?.totalCount = jdbc.queryForObject(filter.getCountQuery(COUNT),
                Long::class.java, *filter.getValues(forCount = true))
    }

    override fun getAll(page: KPage, filter: ExportFilter): KPagedList<Export> {
        filter.page = page // backwards compat
        setCount(filter)
        return KPagedList(filter.page, jdbc.query(filter.getQuery(GET),
                MAPPER, *filter.getValues()))
    }

    override fun count(): Long {
        val user = getUser()
        return jdbc.queryForObject("$COUNT WHERE pk_organization=? AND pk_user_created=?",
                Long::class.java, user.organizationId, user.id)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Export(rs.getObject("pk_export") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getObject("pk_user_created") as UUID,
                    rs.getString("str_name"),
                    rs.getLong("time_created"),
                    PipelineType.Export,
                    JobState.values()[rs.getInt("int_state")])
        }

        private const val COUNT = "SELECT COUNT(1) FROM export "

        private const val GET = "SELECT * FROM export "

        private val INSERT = JdbcUtils.insert("export",
                "pk_export",
                "pk_organization",
                "str_name",
                "pk_user_created",
                "time_created",
                "json_args",
                "json_env")
    }
}
