package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.Job
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface ExportDao {
    fun createExportFile(job: Job, spec: ExportFileSpec): ExportFile

    fun getExportFile(id: UUID): ExportFile

    fun getAllExportFiles(job: Job): List<ExportFile>
}

@Repository
class ExportDaoImpl : AbstractDao(), ExportDao {

    override fun createExportFile(job: Job, spec: ExportFileSpec): ExportFile {
        val id = uuid1.generate()
        try {
            jdbc.update({ connection ->
                val ps = connection.prepareStatement(INSERT)
                ps.setObject(1, id)
                ps.setObject(2, job.jobId)
                ps.setString(3, spec.name)
                ps.setString(4, job.rootPath + "/exported/" + spec.name)
                ps.setString(5, spec.mimeType)
                ps.setLong(6, spec.size)
                ps.setLong(7, System.currentTimeMillis())
                ps
            })
        } catch (e: DuplicateKeyException) {
            throw DuplicateKeyException("The export file " + spec.name
                    + " in job " + job.jobId + " already exists")
        }

        return getExportFile(id)

    }

    override fun getExportFile(id: UUID): ExportFile {
        return jdbc.queryForObject<ExportFile>("$GET WHERE pk_export_file=?", MAPPER_EXPORT_FILE, id)
    }

    override fun getAllExportFiles(job: Job): List<ExportFile> {
        return jdbc.query<ExportFile>("$GET WHERE pk_job=?", MAPPER_EXPORT_FILE, job.jobId)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("export_file",
                "pk_export_file",
                "pk_job",
                "str_name",
                "str_path",
                "str_mime_type",
                "int_size",
                "time_created")

        private val MAPPER_EXPORT_FILE = RowMapper<ExportFile> { rs, _ ->
            ExportFile(rs.getObject("pk_export_file") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getString("str_name"),
                    rs.getString("str_mime_type"),
                    rs.getLong("int_size"),
                    rs.getLong("time_created"))
        }

        private val GET = "SELECT " +
                "pk_export_file," +
                "pk_job," +
                "str_mime_type," +
                "str_name," +
                "str_path," +
                "int_size," +
                "time_created " +
                "FROM " +
                "export_file "
    }
}
