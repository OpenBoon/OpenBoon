package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.*;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobService {

    /**
     * Launches a Job using the given ZPS script. Returns the script
     * back populated with the jobId and first task Id.
     *
     * @param spec
     * @return
     */
    Job launch(JobSpec spec);

    /**
     * Cancel the given job.  The job can be restarted.
     *
     * @param job
     * @return
     */
    boolean cancel(JobId job);

    /**
     * Restart a canceled job.
     *
     * @param job
     * @return
     */
    boolean restart(JobId job);

    boolean createParentDepend(TaskId task);

    Task expand(ExecuteTaskExpand task);

    /**
     * Create a new task.
     *
     * @param script
     * @return
     */
    Task createTask(TaskSpec script);

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
    boolean setTaskState(TaskId task, TaskState newState, TaskState expect);

    /**
     * Set the host the task is running on.
     *
     * @param task
     * @param host
     */
    void setHost(TaskId task, String host);

    /**
     * Set the task state to queued.
     *
     * @param script
     * @return
     */
    boolean setTaskQueued(TaskId script);

    /**
     * Update the task state to finished or succeeded based on the exit status.
     *
     * @param result
     * @return
     */
    boolean setTaskCompleted(ExecuteTaskStopped result);

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

    PagedList<Task> getAllTasks(int job, Paging page);
}
