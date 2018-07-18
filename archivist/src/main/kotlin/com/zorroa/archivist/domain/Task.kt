package com.zorroa.archivist.domain

import java.util.*

enum class TaskState {
    WAITING,
    RUNNING,
    SUCCESS,
    FAILURE,
    SKIPPED;
}

enum class TaskType {
    PIPELINE,
    MANUAL
}

data class TaskSpec(
        var assetId : UUID,
        var pipelineId: UUID?,
        var name: String
)

data class Task(
        val taskId: UUID,
        val assetId: UUID,
        val pipelineId: UUID?,
        val organizationId: UUID,
        val version: Long,
        val name: String)

