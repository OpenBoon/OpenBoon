package com.zorroa.common.domain

import java.util.*

/**
 * @property filename The filename (not the full path)
 * @property document An initial starting document
 * @property pipelines An array of pipelines to run.
 */
data class AssetSpec(
        var filename: String,
        val document: Map<String, Any>? = null,
        var pipelines: List<String>? = null
)

/**
 * @property id The unid ID of the Asset
 * @property organizationId The organization ID
 * @property filename The asset's filename
 */
data class AssetId(
        val id: UUID,
        val organizationId: UUID,
        val filename: String
)


