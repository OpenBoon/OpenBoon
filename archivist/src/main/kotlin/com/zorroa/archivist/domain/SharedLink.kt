package com.zorroa.archivist.domain

import java.util.UUID

class SharedLinkSpec (
        var state: Map<String, Any>? = null,
        var userIds: Set<UUID>? = null,
        var sendEmail : Boolean = false
)

class SharedLink (
        val id: UUID,
        val state : Map<String, Any>,
        val userIds : Set<UUID>? = null
)

