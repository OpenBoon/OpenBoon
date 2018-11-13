package com.zorroa.archivist.domain

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
    Changed
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
        val value: Any?=null
)