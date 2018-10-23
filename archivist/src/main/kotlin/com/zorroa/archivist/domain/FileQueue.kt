package com.zorroa.archivist.domain

import java.util.*

/**
 * Information needed to queue a file spec for processing.
 */
class QueuedFileSpec(
        val organizationId: UUID,
        val pipelineId: UUID,
        val assetId: UUID,
        val path: String,
        val metadata: Map<String, Any>
)

/**
 * A QueuedFile. QueuedFiles come from pub/sub or other unauthed sources.
 */
class QueuedFile(
        val id: UUID,
        val organizationId: UUID,
        val pipelineId: UUID,
        val assetId: UUID,
        val path: String,
        val metadata: Map<String, Any>
)
