package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.domain.*;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.processor.SharedData;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * A service for creating and manipulating jobs.
 */
@Service
@Transactional
public class JobServiceImpl implements JobService {

    private static final Logger logger = LoggerFactory.getLogger(JobServiceImpl.class);

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

    @Autowired
    SharedData sharedData;

    @Autowired
    UserDao userDao;

    /**
     * Creating a job creates both a job record and the initial task.
     *
     * The initial task is a generator.  The generator will create more tasks
     * which map an execution pipeline to each asset generated.  The final task
     * is the reducer.
     *
     * @param spec
     */
    @Override
    public Job launch(JobSpec spec) {
        /**
         * These environment varibles will be set on each task.
         */
        Map<String,String> env = Maps.newHashMap();

        /**
         * The path to the SSL cert needed for tasks to communicate back to archivist.
         */
        env.put("ZORROA_CERT_PATH",
                sharedData.getRootPath().resolve("certs/archivist.p12").toString());
        env.put("ZORROA_USER", SecurityUtils.getUsername());
        env.put("ZORROA_HMAC_KEY", userDao.getHmacKey(SecurityUtils.getUsername()));
        spec.setEnv(env);

        Job job = jobDao.create(spec);

        if (spec.getTasks() != null) {
            for (TaskSpec tspec: spec.getTasks()) {
                createTask(tspec.setJobId(job.getJobId()));
            }
        }
        event.afterCommit(()->
                message.broadcast(new Message("JOB_CREATE", job)));
        return jobDao.get(job.getId());
    }

    @Override
    public boolean cancel(JobId job) {
        if (jobDao.setState(job, JobState.Cancelled, JobState.Active)) {
            event.afterCommit(()->
                    message.broadcast(new Message("JOB_CANCELED",
                            ImmutableMap.of("id", job.getJobId()))));
            return true;
        }
        return false;
    }

    @Override
    public boolean restart(JobId job) {
        if (jobDao.setState(job, JobState.Active, JobState.Cancelled)) {
            event.afterCommit(()->
                    message.broadcast(new Message("JOB_RESTARTED",
                            ImmutableMap.of("id", job.getJobId()))));
            return true;
        }
        return false;
    }

    @Override
    public boolean createParentDepend(TaskId task) {
        return taskDao.createParentDepend(task);
    }

    @Override
    public Task expand(ExecuteTaskExpand expand) {
        if (logger.isDebugEnabled()) {
            logger.debug("Expanding: {}", Json.prettyString(expand));
        }

        TaskSpec spec = new TaskSpec();
        spec.setJobId(expand.getJobId());
        spec.setName(expand.getName());
        spec.setScript(expand.getScript());
        spec.setParentTaskId(expand.getParentTaskId());
        return createTask(spec);
    }

    public Task createTask(TaskSpec spec) {
        /**
         * Create the first task which is just the script itself.
         */

        Task task = taskDao.create(spec);
        logger.info("incrementing waiting tasks: {}", spec.getJobId());
        jobDao.incrementWaitingTaskCount(task);
        taskDao.incrementDependCount(task);
        return task;
    }

    @Override
    public Job get(int id) {
        return jobDao.get(id);
    }

    @Override
    public boolean setTaskState(TaskId task, TaskState newState, TaskState expect) {
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
    public void setHost(TaskId script, String host) {
        taskDao.setHost(script, host);
    }

    @Override
    public boolean setTaskQueued(TaskId script) {
        return setTaskState(script, TaskState.Queued, TaskState.Waiting);
    }

    @Override
    public boolean setTaskCompleted(ExecuteTaskStopped result) {
        TaskState newState = result.getExitStatus() == 0 ? TaskState.Success : TaskState.Failure;
        if (setTaskState(result, newState, TaskState.Running)) {

            if (newState.equals(TaskState.Success)) {
                logger.info("decremented {} depend counts" , taskDao.decrementDependCount(result));
            }

            taskDao.setExitStatus(result, result.getExitStatus());
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

    @Override
    public PagedList<Task> getAllTasks(int job, Paging page) {
        return taskDao.getAll(job, page);
    }
}
