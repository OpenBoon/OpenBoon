package com.zorroa.archivist.domain

import java.util.UUID

class DyHierarchySpec(
    val folderId: UUID,
    val levels: List<DyHierarchyLevel>,
    val generate: Boolean = true
)
