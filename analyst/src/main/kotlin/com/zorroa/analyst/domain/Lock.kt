package com.zorroa.analyst.domain

import com.zorroa.common.domain.Job
import java.util.*

data class LockSpec(
        val assetId: UUID,
        val jobId: UUID
) {
    constructor(job: Job) : this(job.assetId, job.id)

}

data class Lock (
        val id: UUID,
        val assetId: UUID,
        val jobId: UUID
)


