package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.domain.JobId
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*


interface ExportFileDao {
    fun create(job: JobId, spec: ExportFileSpec): ExportFile
    fun get(id: UUID) : ExportFile
    fun getAll(job: JobId): List<ExportFile>
}

@Repository
class ExportFileDaoImpl : AbstractDao(), ExportFileDao {

    override fun create(job: JobId, spec: ExportFileSpec): ExportFile {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, job.jobId)
            ps.setObject(3, getOrgId())
            ps.setString(4, FileUtils.filename(spec.path))
            ps.setString(5, spec.path)
            ps.setString(6, spec.mimeType)
            ps.setLong(7, spec.size)
            ps.setLong(8, time)
            ps
        }

        return get(id)
    }

    override fun get(id: UUID): ExportFile {
        return jdbc.queryForObject("$GET WHERE " +
                "pk_export_file=? AND pk_organization=?",
                MAPPER, id, getOrgId())
    }

    override fun getAll(job: JobId): List<ExportFile> {
        return jdbc.query("$GET WHERE pk_job=? AND pk_organization=?",
                MAPPER, job.jobId, getUser().organizationId)
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

        private const val GET = "SELECT * FROM export_file "

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
