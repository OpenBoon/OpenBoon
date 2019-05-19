package com.zorroa.archivist.domain

import java.util.UUID

/**
 * A [SharedLinkSpec] defines the properties needed for creating
 * a new [SharedLink].
 *
 * @property state: The state of the UI
 * @property userIds: An optional set of UserIds to email.
 */
class SharedLinkSpec(
    var state: Map<String, Any>,
    var userIds: Set<UUID>? = null
)

/**
 * A SharedLink points to a specific curator state that can be shared
 * via a link.
 *
 * @property id The unique of the SharedLink
 * @property state The State of the UI
 * @property userIds: Any users that were notified.
 */
class SharedLink(
    val id: UUID,
    val state: Map<String, Any>,
    val userIds: Set<UUID>? = null
)
