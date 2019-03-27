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
        val assetIds: List<UUID>?=null,
        val userIds: List<UUID>?=null,
        val fieldIds: List<UUID>?=null,
        val timeCreated: LongRangeFilter?=null,
        val types: List<AuditLogType>?=null,
        val attrNames: List<String>?=null
): KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("timeCreated" to "time_created",
                    "userIds" to "pk_user_created",
                    "types" to "int_type",
                    "attrName" to "str_attr_name")

    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_asset", it.size))
            addToValues(it)
        }

        userIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_user_created", it.size))
            addToValues(it)
        }

        fieldIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_field", it.size))
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

        attrNames?.let {
            addToWhere(JdbcUtils.inClause("auditlog.str_attr_name", it.size))
            addToValues(it)
        }

        addToWhere("pk_organization=?")
        addToValues(getOrgId())

    }
}

class AuditLogEntry(
        val id: UUID,
        val assetId: UUID,
        val fieldId: UUID?,
        val user: UserBase,
        val timeCreated: Long,
        val type: AuditLogType,
        val attrName: String?,
        val message: String?,
        val value: Any?
)


class AuditLogEntrySpec(
        val assetId: UUID,
        val type: AuditLogType,
        val fieldId: UUID?=null,
        val message: String?=null,
        val attrName: String?=null,
        val value: Any?=null,
        val scope: String?=null
)
{
    constructor(assetId: String,
                type: AuditLogType,
                fieldId: UUID?=null,
                message: String?=null,
                attrName: String?=null,
                value: Any?=null,
                scope: String?=null) :
            this(UUID.fromString(assetId), type, fieldId, message, attrName, value, scope)

}
