package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.util.*

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
     * @param filter count entries that match the filter.
     * @return The count
     */
    fun count(filter: AuditLogFilter): Long

    /**
     * Get pages of the audit log
     */
    fun getAll(filter: AuditLogFilter) : KPagedList<AuditLogEntry>

}

@Repository
class AuditLogDaoImpl: AbstractDao(), AuditLogDao {

    @Autowired
    lateinit var userDaoCache: UserDaoCache

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    override fun create(spec: AuditLogEntrySpec, kvp: Map<String,Any?>?): AuditLogEntry {
        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()
        val value = Json.serializeToString(spec.value, null)
        val message = spec.message ?: getLogMessage(user, spec, value)

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

    override fun count(filter: AuditLogFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    override fun getAll(filter: AuditLogFilter) : KPagedList<AuditLogEntry> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private fun appendKeyValuePairs(user: UserAuthed, spec: AuditLogEntrySpec, fieldValue: String?) : Map<String, Any> {
        val client = indexRoutingService[user.organizationId]
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
        if (spec.scope != null) {
            map["scope"] = spec.scope
        }
        return map
    }

    private inline fun getEventMessage(spec: AuditLogEntrySpec)  : String {
        val type = spec.type.toString().toLowerCase()
        return "$type Asset"
    }

    private inline fun getLogMessage(user: UserAuthed, spec: AuditLogEntrySpec, fieldValue: String?) : String {
        val scope = spec.scope ?: "index"
        return if (spec.field != null) {
            "${user.username} ${spec.type} during $scope field '${spec.field}' to '$fieldValue' on Asset ${spec.assetId}"
        }
        else {
            "${user.username} ${spec.type} during $scope Asset ${spec.assetId}'"
        }
    }

    private val MAPPER = RowMapper { rs, _ ->
        val json = rs.getString("json_value")
        val fieldValue :Any? = if (json == null) {
            null
        } else {
            Json.deserialize(json, Any::class.java)
        }

        AuditLogEntry(
                rs.getObject("pk_auditlog") as UUID,
                rs.getObject("pk_asset") as UUID,
                userDaoCache.getUser(rs.getObject("pk_user_created") as UUID),
                rs.getLong("time_created"),
                AuditLogType.values()[rs.getInt("int_type")],
                rs.getString("str_message"),
                rs.getString("str_field"),
                fieldValue)
    }


    companion object {

        private val GET = "SELECT " +
                "pk_auditlog,"+
                "pk_asset,"+
                "pk_user_created,"+
                "time_created,"+
                "int_type,"+
                "str_message,"+
                "str_field,"+
                "json_value " +
                "FROM auditlog"


        private val COUNT = "SELECT COUNT(1) FROM auditlog"

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