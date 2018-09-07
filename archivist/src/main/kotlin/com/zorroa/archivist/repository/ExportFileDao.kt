package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Export
import com.zorroa.archivist.domain.ExportFile
import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*


interface ExportFileDao {
    fun create(export: Export, spec: ExportFileSpec): ExportFile
    fun get(id: UUID) : ExportFile
    fun getAll(export: Export): List<ExportFile>
}

@Repository
class ExportFileDaoImpl : AbstractDao(), ExportFileDao {

    override fun create(export: Export, spec: ExportFileSpec): ExportFile {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, export.id)
            ps.setString(3, FileUtils.filename(spec.path))
            ps.setString(4, spec.path)
            ps.setString(5, spec.mimeType)
            ps.setLong(6, spec.size)
            ps.setLong(7, time)
            ps
        }

        return get(id)
    }

    override fun get(id: UUID): ExportFile {
        return jdbc.queryForObject("$GET WHERE " +
                "pk_export_file=? AND export.pk_organization=?",
                MAPPER, id, getUser().organizationId)
    }

    override fun getAll(export: Export): List<ExportFile> {
        return jdbc.query("$GET WHERE export_file.pk_export=? AND export.pk_organization=?",
                MAPPER, export.id, getUser().organizationId)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            ExportFile(rs.getObject("pk_export_file") as UUID,
                    rs.getObject("pk_export") as UUID,
                    rs.getString("str_name"),
                    rs.getString("str_path"),
                    rs.getString("str_mime_type"),
                    rs.getLong("int_size"),
                    rs.getLong("time_created"))
        }

        private const val GET = "SELECT * " +
                "FROM export_file " +
                "INNER JOIN export ON (export.pk_export = export_file.pk_export) "

        private val INSERT = JdbcUtils.insert("export_file",
                "pk_export_file",
                "pk_export",
                "str_name",
                "str_path",
                "str_mime_type",
                "int_size",
                "time_created")
    }

}
