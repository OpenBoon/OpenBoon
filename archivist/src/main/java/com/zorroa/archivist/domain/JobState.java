package com.zorroa.archivist.domain;

/**
 * In the DB the job state can only be 0 or 1, aka Running or Finished.
 * Other states may be sent to the client,
 */
public enum JobState {

    /**
     * The job has tasks running or queued.
     */
    Active,

    /**
     * The job was manually cancelled.
     */
    Cancelled,

    /**
     * The job finished naturally.  This is a psuedo state and is never set
     * on the job but calculated based on the pending task list.
     */
    Finished,

    /**
     * Job is expired and no longer restartable.
     */
    Expired,

    /**
     * Job is waiting to run.
     */
    Waiting,

    /**
     * All tasks failed.
     */
    Failed
}
