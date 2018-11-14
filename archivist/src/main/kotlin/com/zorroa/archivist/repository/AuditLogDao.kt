package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.archivist.util.event
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement

interface AuditLogDao {

    /**
     * Create a new audit log entry.
     * @param spec - an AuditLogEntrySpec
     * @param kvp: additional key/value pairs for logging
     * @return AuditLogEntry
     */
    fun create(spec: AuditLogEntrySpec, kvp: Map<String, Any?>?=null): AuditLogEntry

    /**
     * Batch create an array of audit log entries.
     * @param specs: a list of AuditLogEntrySpecs
     * @param kvp: additional key/value pairs for logging
     * @return the number of specs inserted
     */
    fun batchCreate(specs: List<AuditLogEntrySpec>, kvp: Map<String, Any?>?=null) : Int

    /**
     * Batch create an array of audit log entries.
     * @param assetId count entries in log for a given assset ID
     * @return The count
     */
    fun count(assetId: String) : Int
}

@Repository
class AuditLogDaoImpl: AbstractDao(), AuditLogDao {

    @Autowired
    lateinit var userDaoCache: UserDaoCache

    @Autowired
    lateinit var esClientCache: EsClientCache

    override fun create(spec: AuditLogEntrySpec, kvp: Map<String,Any?>?): AuditLogEntry {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()
        val value = Json.serializeToString(spec.value, null)
        val message = spec.message ?: getLogMessage(user, spec, value)

        logger.event(message, kvp, appendKeyValuePairs(user, spec, value))

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

    override fun batchCreate(specs: List<AuditLogEntrySpec>, kvp: Map<String, Any?>?) : Int {

        val time = System.currentTimeMillis()
        val user = getUser()

        val result = jdbc.batchUpdate(INSERT, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val spec = specs[i]
                val value = Json.serializeToString(spec.value, null)
                val message = spec.message ?: getLogMessage(user, spec, value)

                logger.event(getEventMessage(spec), kvp, appendKeyValuePairs(user, spec, value))

                ps.setObject(1, uuid1.generate())
                ps.setObject(2, spec.assetId)
                ps.setObject(3, user.organizationId)
                ps.setObject(4, user.id)
                ps.setLong(5, time)
                ps.setInt(6, spec.type.ordinal)
                ps.setString(7, spec.field)
                ps.setString(8, value)
                ps.setString(9, message)
            }

            override fun getBatchSize(): Int {
                return specs.size
            }
        })
        return result.sum()
    }

    override fun count(assetId: String): Int {
       return jdbc.queryForObject("SELECT COUNT(1) FROM auditlog WHERE pk_asset=?::uuid",
               Int::class.java, assetId)
    }

    private fun appendKeyValuePairs(user: UserAuthed, spec: AuditLogEntrySpec, fieldValue: String?) : Map<String, Any> {
        val client = esClientCache[user.organizationId]
        val map = mutableMapOf<String,Any>()
        map["index"] = client.route.indexName
        map["cluster"] = client.route.clusterUrl
        map["assetId"] = spec.assetId
        if (spec.field != null) {
            map["field"] = spec.field
        }
        if (fieldValue != null) {
            map["fieldValue"] = fieldValue
        }
        return map
    }

    private inline fun getEventMessage(spec: AuditLogEntrySpec)  : String {
        val type = spec.type.toString().toLowerCase()
        return "$type Asset"
    }

    private inline fun getLogMessage(user: UserAuthed, spec: AuditLogEntrySpec, fieldValue: String?)  : String {
        return if (spec.field != null) {
            "${user.username} ${spec.type} field '${spec.field}' to '$fieldValue' on Asset ${spec.assetId}"
        }
        else {
            "${user.username} ${spec.type} Asset ${spec.assetId}'"
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