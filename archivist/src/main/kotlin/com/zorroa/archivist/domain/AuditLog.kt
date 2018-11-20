package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.LongRangeFilter
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

/**
 * The type of audit log entry.
 */
enum class AuditLogType {
    /**
     * The initial creation of an asset.
     */
    Created,

    /**
     * The asset was replaced.
     */
    Replaced,

    /**
     * The asset was deleted.
     */
    Deleted,

    /**
     * Permissions on the asset changed.
     */
    Secured,

    /**
     * A field on an asset changed in some way.
     */
    Changed,

    /**
     * A Warning message concerning the asset.
     */
    Warning
}


class AuditLogFilter(
        val assetIds: Set<UUID>?=null,
        val userIds: Set<UUID>?=null,
        val timeCreated: LongRangeFilter?=null,
        val types: Set<AuditLogType>?=null,
        val fields: Set<String>?=null
): KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("timeCreated" to "time_created",
                    "userIds" to "pk_user_created",
                    "types" to "int_type",
                    "fields" to "str_field")

    override fun build() {

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_asset", it.size))
            addToValues(it)
        }

        userIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_user_created", it.size))
            addToValues(it)
        }

        timeCreated?.let {
            addToWhere(JdbcUtils.rangeClause("auditlog.time_created", it))
            addToValues(it.getFilterValues())
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("auditlog.int_type", it.size))
            addToValues(it.map { t-> t.ordinal })
        }

        fields?.let {
            addToWhere(JdbcUtils.inClause("auditlog.str_field", it.size))
            addToValues(it)
        }

        addToWhere("pk_organization=?")
        addToValues(getOrgId())

    }
}

class AuditLogEntry(
        val id: UUID,
        val assetId: UUID,
        val user: UserBase,
        val timeCreated: Long,
        val type: AuditLogType,
        val message: String?,
        val field: String?,
        val value: Any?
)


class AuditLogEntrySpec(
        val assetId: UUID,
        val type: AuditLogType,
        val message: String?=null,
        val field: String?=null,
        val value: Any?=null,
        val scope: String?=null
)
{
    constructor(assetId: String,
                type: AuditLogType,
                message: String?=null,
                field: String?=null,
                value: Any?=null,
                scope: String?=null) :
            this(UUID.fromString(assetId), type, message, field, value, scope)
}
