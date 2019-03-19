package com.zorroa.archivist.domain

import com.zorroa.common.domain.TaskState
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
        val exitStatus: Int,
        val newState: TaskState?= null,
        val manualKill: Boolean = false
)

/**
 * TaskErrorEvents are emitted by the processing system if an an exception is thrown
 * while processing.
 *
 * @property assetId The assetID that was being processed.  This is optional.
 * @property path The path that was being processed.  This is optional.
 * @property message The message from the exception that generated the event, or a custom message.
 * @property processor The processor class the error occurred in.
 * @property fatal True if the error was fatal.
 * @property phase The phase at which the error occurred: generate, execute, teardown.
 * @property stackTrace The full stack trace from the error, if any. This is optional.
 */
class TaskErrorEvent(
        val assetId: UUID?,
        val path: String?,
        val message: String,
        val processor: String,
        val fatal: Boolean,
        val phase: String,
        val stackTrace: List<StackTraceElement>?=null
)

class TaskMessageEvent(
        val message: String
)