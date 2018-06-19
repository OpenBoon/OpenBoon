package com.zorroa.irm.studio.domain

import java.util.*

enum class JobState {
    WAITING,
    RUNNING,
    SUCCESS,
    FAIL
}

data class JobSpec (
        val name: String,
        val assetId: UUID,
        val organizationId: UUID,
        val pipelines: List<String>
)

data class Job (
        val id: UUID,
        val assetId: UUID,
        val organizationId: UUID,
        val name: String,
        val state: JobState,
        val pipelines: List<String>
)
