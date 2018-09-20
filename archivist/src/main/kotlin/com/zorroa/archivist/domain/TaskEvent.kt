package com.zorroa.archivist.domain

import java.util.*

enum class TaskEventType {
    STOPPED,
    STARTED,
    ERROR,
    EXPAND,
    MESSAGE
}

open class TaskEvent(
        val type: TaskEventType,
        val taskId: UUID,
        val jobId: UUID,
        val payload: Any)

class TaskStoppedEvent(
        val exitStatus: Int
)

class TaskErrorEvent(
        val assetId: UUID?,
        val path: String?,
        val message: String,
        val processor: String,
        val fatal: Boolean,
        val phase: String
)

class TaskMessageEvent(
        val message: String
)