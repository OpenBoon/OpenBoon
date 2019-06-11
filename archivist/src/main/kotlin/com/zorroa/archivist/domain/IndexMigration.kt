package com.zorroa.archivist.domain

import java.util.UUID

/**
 * An IndexMigrationSpec allows users to migrate data from one index route to another.  Both
 * route's have to to exist.
 */
class IndexMigrationSpec (
    val dstRouteId: UUID,
    val swapRoutes: Boolean = true,
    val removeAttrs: List<String>?=null,
    val setAttrs: Map<String, Any>?=null
)


