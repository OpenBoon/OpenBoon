package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.ServableFile
import com.zorroa.archivist.service.event
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobId
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface ExportFileDao {
    fun create(job: JobId, file: ServableFile, spec: ExportFileSpec): ExportFile
    fun get(id: UUID): ExportFile
    fun getAll(job: JobId): List<ExportFile>
}

@Repository
class ExportFileDaoImpl : AbstractDao(), ExportFileDao {

    override fun create(job: JobId, file: ServableFile, spec: ExportFileSpec): ExportFile {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val stat = file.getStat()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, job.jobId)
            ps.setObject(3, getOrgId())
            ps.setString(4, spec.filename)
            ps.setString(5, spec.storageId)
            ps.setString(6, stat.mediaType)
            ps.setLong(7, stat.size)
            ps.setLong(8, time)
            ps
        }

        logger.event(
            LogObject.EXPORT_FILE, LogAction.CREATE,
                mapOf("jobId" to job.jobId, "storageId" to spec.storageId))
        return get(id)
    }

    override fun get(id: UUID): ExportFile {
        val user = getUser()
        return jdbc.queryForObject("$GET WHERE " +
                "pk_export_file=? AND export_file.pk_organization=? AND job.pk_user_created=?",
                MAPPER, id, user.organizationId, user.id)
    }

    override fun getAll(job: JobId): List<ExportFile> {
        val user = getUser()
        return jdbc.query("$GET WHERE " +
            "export_file.pk_job=? AND export_file.pk_organization=? " +
            "AND job.pk_user_created=? ORDER BY export_file.time_created DESC",
                MAPPER, job.jobId, user.organizationId, user.id)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            ExportFile(rs.getObject("pk_export_file") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getString("str_name"),
                    rs.getString("str_path"),
                    rs.getString("str_mime_type"),
                    rs.getLong("int_size"),
                    rs.getLong("time_created"))
        }

        private const val GET = "SELECT * FROM export_file " +
            "INNER JOIN job ON (job.pk_job = export_file.pk_job)"

        private val INSERT = JdbcUtils.insert("export_file",
                "pk_export_file",
                "pk_job",
                "pk_organization",
                "str_name",
                "str_path",
                "str_mime_type",
                "int_size",
                "time_created")
    }
}
