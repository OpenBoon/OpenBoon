package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.archivist.util.event
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement

interface AuditLogDao {

    /**
     * Create a new audit log entry.
     * @param spec - an AuditLogEntrySpec
     * @return AuditLogEntry
     */
    fun create(spec: AuditLogEntrySpec): AuditLogEntry

    /**
     * Batch create an array of audit log entries.
     * @param specs: a list of AuditLogEntrySpecs
     * @return the number of specs inserted
     */
    fun batchCreate(specs: List<AuditLogEntrySpec>) : Int
}

@Repository
class AuditLogDaoImpl: AbstractDao(), AuditLogDao {

    @Autowired
    internal lateinit var userDaoCache: UserDaoCache

    override fun create(spec: AuditLogEntrySpec): AuditLogEntry {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()
        val value = Json.serializeToString(spec.value, null)
        val message = spec.message ?: getAutoMessage(user, spec, value)

        logger.event(message, mapOf())

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, spec.assetId)
            ps.setObject(3, user.organizationId)
            ps.setObject(4, user.id)
            ps.setLong(5, time)
            ps.setInt(6, spec.type.ordinal)
            ps.setString(7, spec.field)
            ps.setString(8, value)
            ps.setString(9, message)
            ps
        }

        return AuditLogEntry(
                id,
                spec.assetId,
                userDaoCache.getUser(user.id),
                time,
                spec.type,
                message,
                spec.field,
                value)
    }

    override fun batchCreate(specs: List<AuditLogEntrySpec>) : Int {

        val time = System.currentTimeMillis()
        val user = getUser()

        val result = jdbc.batchUpdate(INSERT, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val spec = specs[i]
                val value = Json.serializeToString(spec.value, null)
                val message = spec.message ?: getAutoMessage(user, spec, value)
                logger.event(message, mapOf())

                ps.setObject(1, uuid1.generate())
                ps.setObject(2, spec.assetId)
                ps.setObject(3, user.organizationId)
                ps.setObject(4, user.id)
                ps.setLong(5, time)
                ps.setInt(6, spec.type.ordinal)
                ps.setString(7, spec.field)
                ps.setString(8, value)
                ps.setString(9,  message)
            }

            override fun getBatchSize(): Int {
                return specs.size
            }
        })
        return result.sum()
    }

    fun getAutoMessage(user: UserAuthed, spec: AuditLogEntrySpec, fieldValue: String?)  : String {
        return if (spec.field != null) {
            "username='${user.username}' ${spec.type} field='${spec.field}' to value='$fieldValue' on assetId='${spec.assetId}'"
        }
        else {
            "username='${user.username}' ${spec.type} assetId='{$spec.assetId}'"
        }
    }

    companion object {

        private val INSERT = JdbcUtils.insert("auditlog",
                "pk_auditlog",
                "pk_asset",
                "pk_organization",
                "pk_user_created",
                "time_created",
                "int_type",
                "str_field",
                "json_value::jsonb",
                "str_message")
    }

}