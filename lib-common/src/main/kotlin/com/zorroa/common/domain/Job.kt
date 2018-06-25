package com.zorroa.common.domain

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
        val pipelines: List<String>,
        val attrs: Map<String, Any>? = null,
        val env: Map<String, String>? = null
)

data class Job (
        val id: UUID,
        val assetId: UUID,
        val organizationId: UUID,
        val name: String,
        val state: JobState,
        val pipelines: List<String>,
        val attrs: Map<String, Any> = mutableMapOf(),
        val env: Map<String, String> = mutableMapOf()
)
