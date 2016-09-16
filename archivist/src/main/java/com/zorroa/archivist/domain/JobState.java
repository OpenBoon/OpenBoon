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
     * The job finished naturally.
     */
    Finished
}
