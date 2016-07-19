package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.zps.ZpsJob;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service for creating and manipulating jobs.
 */
@Service
@Transactional
public class JobServiceImpl implements JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobExecutorServiceImpl.class);

    @Autowired
    JobDao jobDao;

    @Autowired
    TaskDao taskDao;

    @Autowired
    MessagingService message;

    @Autowired
    DyHierarchyService dyHierarchyService;

    @Autowired
    TransactionEventManager event;

    /**
     * Creating a job creates both a job record and the initial task.
     *
     * The initial task is a generator.  The generator will create more tasks
     * which map an execution pipeline to each asset generated.  The final task
     * is the reducer.
     *
     * @param job
     */
    @Override
    public ZpsScript launch(ZpsScript job, PipelineType type) {
        job = jobDao.create(job, type);
        job = createTask(job);
        final int id = job.getJobId();
        event.afterCommit(()->
                message.broadcast(new Message("JOB_CREATE",
                        ImmutableMap.of("type", type, "id", id))));
        return job;
    }

    @Override
    public boolean cancel(ZpsJob job) {
        if (jobDao.setState(job, JobState.Cancelled, JobState.Active)) {
            event.afterCommit(()->
                    message.broadcast(new Message("JOB_CANCELED",
                            ImmutableMap.of("id", job.getJobId()))));
            return true;
        }
        return false;
    }

    @Override
    public boolean restart(ZpsJob job) {
        if (jobDao.setState(job, JobState.Active, JobState.Cancelled)) {
            event.afterCommit(()->
                    message.broadcast(new Message("JOB_RESTARTED",
                            ImmutableMap.of("id", job.getJobId()))));
            return true;
        }
        return false;
    }

    public ZpsScript createTask(ZpsScript script) {
        /**
         * Create the first task which is just the script itself.
         */
        taskDao.create(script);
        jobDao.incrementWaitingTaskCount(script);
        return script;
    }

    @Override
    public Job get(int id) {
        return jobDao.get(id);
    }

    @Override
    public boolean setTaskState(ZpsTask task, TaskState newState, TaskState expect) {
        Preconditions.checkNotNull(task.getTaskId());
        Preconditions.checkNotNull(task.getJobId());

        if (taskDao.setState(task, newState, expect)) {
            if (jobDao.updateTaskStateCounts(task, newState, expect).equals(JobState.Finished)) {
                dyHierarchyService.submitGenerateAll(true);
            }
            return true;
        }
        return false;
    }

    @Override
    public void setHost(ZpsTask script, String host) {
        taskDao.setHost(script, host);
    }

    @Override
    public boolean setTaskQueued(ZpsTask script) {
        return setTaskState(script, TaskState.Queued, TaskState.Waiting);
    }

    @Override
    public boolean setTaskCompleted(ZpsTask script, int exitStatus) {
        TaskState newState = exitStatus == 0 ? TaskState.Success : TaskState.Failure;
        logger.info("Task {} completed, new state {}", script.getTaskId(), newState);
        if (setTaskState(script, newState, TaskState.Running)) {
            logger.info("Setting exist status: {}", exitStatus);
            taskDao.setExitStatus(script, exitStatus);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateStats(int id, int created, int updated, int errors, int warnings) {
        return jobDao.incrementStats(id, created, updated, errors, warnings);
    }

    @Override
    public PagedList<Job> getAll(Paging page, JobFilter filter) {
        return jobDao.getAll(page, filter);
    }
}
