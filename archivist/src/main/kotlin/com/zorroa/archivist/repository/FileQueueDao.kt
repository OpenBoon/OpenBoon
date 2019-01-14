package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.QueuedFile
import com.zorroa.archivist.domain.QueuedFileSpec
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*


interface FileQueueDao {
    fun create(spec: QueuedFileSpec): QueuedFile
    fun get(id: UUID) : QueuedFile
    fun getAll(limit: Int) : List<QueuedFile>
    fun delete(files: List<QueuedFile>) : Int
    fun getOrganizationMeters() : Map<String, Long>

}

@Repository
class FileQueueDaoImpl : AbstractDao(), FileQueueDao {

    override fun delete(files: List<QueuedFile>) : Int {
        return jdbc.batchUpdate("DELETE FROM queued_file WHERE pk_queued_file=?",
                object : BatchPreparedStatementSetter {

            @Throws(SQLException::class)
            override fun setValues(ps: PreparedStatement, i: Int) {
                ps.setObject(1, files[i].id)
            }

            override fun getBatchSize(): Int {
                return files.size
            }
        }).size
    }

    override fun getAll(limit: Int) : List<QueuedFile> {
        return jdbc.query("$GET ORDER BY pk_organization, pk_pipeline LIMIT ?", MAPPER, limit)
    }

    override fun get(id: UUID) : QueuedFile {
        return jdbc.queryForObject("$GET WHERE pk_queued_file=?", MAPPER, id)
    }

    override fun getOrganizationMeters() : Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        jdbc.query(ORG_METERS) { rs->
            result.put(rs.getString(1), rs.getLong(2))
        }
        return result
    }

    override fun create(spec: QueuedFileSpec): QueuedFile {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.organizationId)
            ps.setObject(3, spec.pipelineId)
            ps.setObject(4, spec.assetId)
            ps.setString(5, Json.serializeToString(spec.metadata, "{}"))
            ps.setString(6, spec.path)
            ps.setLong(7, time)
            ps
        }

        logger.event(LogObject.FILEQ, LogAction.CREATE,
                mapOf("assetPath" to spec.path,
                    "assetId" to spec.assetId,
                    "pipelineId" to spec.pipelineId))

        return QueuedFile(id, spec.organizationId, spec.pipelineId, spec.assetId, spec.path, spec.metadata)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            QueuedFile(
                    rs.getObject("pk_queued_file") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getObject("pk_pipeline") as UUID,
                    rs.getObject("asset_id") as UUID,
                    rs.getString("str_path"),
                    Json.deserialize(rs.getString("json_metadata"), Json.GENERIC_MAP))
        }

        private const val GET = "SELECT " +
                "pk_queued_file, " +
                "pk_organization, " +
                "pk_pipeline, " +
                "asset_id,"+
                "json_metadata,"+
                "str_path " +
                "FROM " +
                "queued_file "

        private val INSERT = JdbcUtils.insert("queued_file",
                "pk_queued_file",
                "pk_organization",
                "pk_pipeline",
                "asset_id",
                "json_metadata",
                "str_path",
                "time_created")

        private const val ORG_METERS =
                "SELECT " +
                    "org.str_name, " +
                    "COUNT(1) " +
                "FROM " +
                    "queued_file qf INNER JOIN organization org ON (qf.pk_organization = org.pk_organization) " +
                "GROUP BY " +
                    "org.str_name"
    }
}