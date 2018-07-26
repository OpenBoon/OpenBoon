package com.zorroa.analyst.domain

import java.util.*

data class LockSpec(
        val assetId: UUID,
        val jobId: UUID
) {


}

data class Lock (
        val id: UUID,
        val assetId: UUID,
        val jobId: UUID
)


