package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;
import com.zorroa.common.cluster.thrift.ExpandT;
import com.zorroa.archivist.domain.JobId;
import com.zorroa.archivist.domain.TaskId;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by chambers on 6/24/16.
 */
public interface JobService {

    Job launch(JobSpecV spec);

    /**
     * Launches a Job using the given ZPS script. Returns the script
     * back populated with the jobId and first task Id.
     *
     * @param spec
     * @return
     */
    Job launch(JobSpec spec);

    boolean createParentDepend(TaskId task);

    List<Task> expand(Task task, ExpandT expand);

    /**
     * Create a new task with a validated TaskSpecV.  The validated spec
     * can only reference a pipeline.
     *
     * @param spec
     * @return
     */
    Task createTask(TaskSpecV spec);

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
     * Set the state of a given job.
     *
     * @param job
     * @param newState
     * @param oldState
     * @return
     */
    boolean setJobState(JobId job, JobState newState, JobState oldState);

    boolean setTaskState(TaskId task, TaskState newState);

    /**
     * Set the state of a given task.  The current state must be the expected state.
     *
     * @param task
     * @param newState
     * @param expect
     * @return
     */
    boolean setTaskState(TaskId task, TaskState newState, TaskState ... expect);

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
     * @return
     */
    boolean setTaskQueued(TaskId id);

    boolean setTaskQueued(TaskId script, String host);

    boolean setTaskCompleted(Task task, int exitStatus);
    /**
     * Increment asset related stats.
     *
     * @param id
     * @param success
     * @param errors
     * @param warnings
     * @return
     */
    boolean incrementJobStats(int id, int success, int errors, int warnings);

    boolean incrementTaskStats(int id, int success, int errors, int warnings);

    /**
     * Return a list of jobs matching the given criteria.
     *
     * @param page
     * @param filter
     * @return
     */
    PagedList<Job> getAll(Pager page, JobFilter filter);

    PagedList<Task> getAllTasks(int job, Pager page);

    int updatePingTime(List<Integer> taskIds);

    Path resolveJobRoot(JobSpec spec);
}
