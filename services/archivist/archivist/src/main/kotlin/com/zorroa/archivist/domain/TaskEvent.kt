package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

enum class TaskEventType {
    STOPPED,
    STARTED,
    ERROR,
    EXPAND,
    MESSAGE,
    STATS,
    INDEX,
    PROGRESS,
    STATUS
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
    val manualKill: Boolean = false,
    val message: String? = null
)

class TaskProgressUpdateEvent(
    val progress: Int,
    val status: String? = null
)

class TaskStatusUpdateEvent(
    val status: String
)

/**
 * An event which defines new asset specs to process.
 *
 * @param assets The list of asset specs.
 */
class TaskExpandEvent(
    val assets: List<AssetSpec>
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
@ApiModel(
    "TaskErrorEvent",
    description = "TaskErrorEvents are emitted by the processing system if an an exception is thrown while processing"
)
class TaskErrorEvent(
    @ApiModelProperty("The assetID that was being processed.", required = false)
    val assetId: String?,
    @ApiModelProperty("The path that was being processed.", required = false)
    val path: String?,
    @ApiModelProperty("The message from the exception that generated the event, or a custom message.")
    val message: String,
    @ApiModelProperty("The processor class the error occurred in.")
    val processor: String?,
    @ApiModelProperty("True if the error was fatal.")
    val fatal: Boolean,
    @ApiModelProperty("The phase at which the error occurred: generate, execute, teardown.")
    val phase: String,
    @ApiModelProperty("The full stack trace from the error, if any.", required = false)
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
@ApiModel(
    "TaskStatsEvent",
    description = "The TaskStatsEvent contains the run count, and min/max/avg exec times for each processor of a given task.  Emitted by the processing system once a task is completed."
)
class TaskStatsEvent(
    @ApiModelProperty("The processor name")
    val processor: String,
    @ApiModelProperty("The number of times the processor was run.")
    val count: Long,
    @ApiModelProperty("The lowest time it took the processor to run.")
    val min: Double,
    @ApiModelProperty("The maximum time it took the processor to run.")
    val max: Double,
    @ApiModelProperty("The average time it took the processor to run.")
    val avg: Double
)

@ApiModel(
    "IndexAssetsEvent",
    description = "An event containing assets to index"
)
class BatchIndexAssetsEvent(
    @ApiModelProperty("A list of documents to index.")
    val assets: Map<String, MutableMap<String, Any>>,

    @ApiModelProperty("The task settings, if any.  Used for various types of behavior switching ")
    val settings: Map<String, Any>?
)
