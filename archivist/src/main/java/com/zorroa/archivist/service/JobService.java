package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobFilter;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobService {

    /**
     * Launches a Job using the given ZPS script. Returns the script
     * back populated with the jobId and first task Id.
     *
     * @param script
     * @param type
     * @return
     */
    ZpsScript launch(ZpsScript script, PipelineType type);

    /**
     * Create a new task.
     *
     * @param script
     * @return
     */
    ZpsScript createTask(ZpsScript script);

    /**
     * Get a job by id.
     *
     * @param id
     * @return
     */
    Job get(int id);

    /**
     * Set the state of a given task.  The current state must be the expected state.
     *
     * @param task
     * @param newState
     * @param expect
     * @return
     */
    boolean setTaskState(ZpsTask task, TaskState newState, TaskState expect);

    /**
     * Set the host the task is running on.
     *
     * @param task
     * @param host
     */
    void setHost(ZpsTask task, String host);

    /**
     * Set the task state to queued.
     *
     * @param script
     * @return
     */
    boolean setTaskQueued(ZpsTask script);

    /**
     * Update the task state to finished or succeeded based on the exit status.
     *
     * @param script
     * @param exitStatus
     * @return
     */
    boolean setTaskCompleted(ZpsTask script, int exitStatus);

    /**
     * Increment asset related stats.
     *
     * @param id
     * @param created
     * @param updated
     * @param errors
     * @param warnings
     * @return
     */
    boolean updateStats(int id, int created, int updated, int errors, int warnings);

    /**
     * Return a list of jobs matching the given criteria.
     *
     * @param page
     * @param filter
     * @return
     */
    PagedList<Job> getAll(Paging page, JobFilter filter);

}
