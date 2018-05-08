package com.zorroa.archivist.domain;

import java.util.UUID;

/**
 * The minimal propertied needed to be considered a task.
 */
public interface TaskId extends JobId {

    /**
     * Return the task id.
     * @return
     */
    UUID getTaskId();

    /**
     * Return Id of the task that created this task.
     *
     * @return
     */
    UUID getParentTaskId();
}
