package com.zorroa.archivist.domain

import java.util.*

class DyHierarchySpec(
        val folderId: UUID,
        val levels: List<DyHierarchyLevel>,
        val generate: Boolean = true
)
