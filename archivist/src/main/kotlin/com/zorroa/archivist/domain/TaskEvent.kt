package com.zorroa.archivist.domain

import java.util.*

open class TaskEvent(
        val type: String,
        val endpoint: String,
        val taskId: UUID,
        val jobId: UUID,
        val payload: Any)

class TaskStoppedEvent(
        val exitStatus: Int
)
