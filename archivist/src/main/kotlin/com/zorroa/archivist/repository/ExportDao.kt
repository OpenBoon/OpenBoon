package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.Job
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface ExportDao {
    fun createExportFile(job: Job, spec: ExportFileSpec): ExportFile

    fun getExportFile(id: Long): ExportFile

    fun getAllExportFiles(job: Job): List<ExportFile>
}

@Repository
open class ExportDaoImpl : AbstractDao(), ExportDao {

    override fun createExportFile(job: Job, spec: ExportFileSpec): ExportFile {
        val keyHolder = GeneratedKeyHolder()
        try {
            jdbc.update({ connection ->
                val ps = connection.prepareStatement(INSERT, arrayOf("pk_export_file"))
                ps.setLong(1, job.jobId!!.toLong())
                ps.setString(2, spec.name)
                ps.setString(3, job.rootPath + "/exported/" + spec.name)
                ps.setString(4, spec.mimeType)
                ps.setLong(5, spec.size)
                ps.setLong(6, System.currentTimeMillis())
                ps
            }, keyHolder)
        } catch (e: DuplicateKeyException) {
            throw DuplicateKeyException("The export file " + spec.name
                    + " in job " + job.jobId + " already exists")
        }

        val id = keyHolder.key.toInt()
        return getExportFile(id.toLong())

    }

    override fun getExportFile(id: Long): ExportFile {
        return jdbc.queryForObject<ExportFile>(GET + " WHERE pk_export_file=?", MAPPER_EXPORT_FILE, id)
    }

    override fun getAllExportFiles(job: Job): List<ExportFile> {
        return jdbc.query<ExportFile>(GET + " WHERE pk_job=?", MAPPER_EXPORT_FILE, job.jobId)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("export_file",
                "pk_job",
                "str_name",
                "str_path",
                "str_mime_type",
                "int_size",
                "time_created")

        private val MAPPER_EXPORT_FILE = RowMapper<ExportFile> { rs, _ ->
            ExportFile(rs.getLong("pk_export_file"),
                    rs.getLong("pk_job"),
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
