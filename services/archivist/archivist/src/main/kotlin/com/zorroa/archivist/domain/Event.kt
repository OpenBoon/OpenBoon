package com.zorroa.archivist.domain

/**
 * Internal event bus events
 */

/**
 * Emitted when watermark settings have changed.
 */
class WatermarkSettingsChanged

/**
 * Emitted when a job state changes.
 */
class JobStateChangeEvent(val job: Job, val newState: JobState, val oldState: JobState?)

/**
 * Emitted when a task state changes.
 */
class TaskStateChangeEvent(val task: InternalTask, val newState: TaskState, val oldState: TaskState?)
