package com.zorroa.archivist.domain;

/**
 * The minimal propertied needed to be considered a task.
 */
public interface TaskId extends JobId {

    /**
     * Return the task id.
     * @return
     */
    Integer getTaskId();

    /**
     * Return Id of the task that created this task.
     *
     * @return
     */
    Integer getParentTaskId();
}
