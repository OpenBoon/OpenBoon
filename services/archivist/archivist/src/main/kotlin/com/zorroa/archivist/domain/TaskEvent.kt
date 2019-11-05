package com.zorroa.archivist.domain

import java.util.UUID

enum class TaskEventType {
    STOPPED,
    STARTED,
    ERROR,
    EXPAND,
    MESSAGE,
    STATS
}

open class TaskEvent(
    val type: TaskEventType,
    val taskId: UUID,
    val jobId: UUID,
    val payload: Any
)

class TaskStoppedEvent(
    val exitStatus: Int,
    val newState: TaskState? = null,
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
    val processor: String?,
    val fatal: Boolean,
    val phase: String,
    val stackTrace: List<StackTraceElement>? = null
)

class TaskMessageEvent(
    val message: String
)

/**
 * The TaskStatsEvent contains the run count, and min/max/avg exec times for each processor of a
 * given task.  Emitted by the processing system once a task is completed.
 *
 * @property processor The processor name
 * @property count The number of times the processor was run.
 * @property min The lowest time it took the processor to run.
 * @property max The maximum time it took the processor to run.
 * @property avg The average time it took the processor to run.
 */
class TaskStatsEvent(
    val processor: String,
    val count: Long,
    val min: Double,
    val max: Double,
    val avg: Double
)