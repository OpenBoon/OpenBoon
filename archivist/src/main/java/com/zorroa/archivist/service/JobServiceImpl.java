package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.tx.TransactionEventManager;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.*;
import com.zorroa.sdk.domain.Message;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

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
    UserDao userDao;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    AnalystService analystService;

    @Autowired
    PluginService pluginService;

    /**
     * Launch a validated JobSpec provided by REST endpoint.
     * @param specv
     * @return
     */
    @Override
    public Job launch(JobSpecV specv) {

        if (!JdbcUtils.isValid(specv.getScript().getGenerate()) &&
               !JdbcUtils.isValid(specv.getScript().getOver())) {
            throw new IllegalArgumentException("Script has neither data to iterate over or a generator");
        }

        if (!JdbcUtils.isValid(specv.getScript().getExecute())) {
            throw new IllegalArgumentException("Script has no execute clause.");
        }

        /**
         * Validates processors actually exists.
         */
        specv.getScript().setGenerate(
                pluginService.getProcessorRefs(specv.getScript().getGenerate()));
        specv.getScript().setExecute(
                pluginService.getProcessorRefs(specv.getScript().getExecute()));

        TaskSpec tspec = new TaskSpec();
        tspec.setName(specv.getName());
        tspec.setScript(Json.serializeToString(specv.getScript()));

        JobSpec spec = new JobSpec();
        spec.setName(specv.getName());
        spec.setType(specv.getType());
        spec.setArgs(specv.getArgs());
        spec.setEnv(specv.getEnv());
        spec.setArgs(specv.getArgs());
        spec.setTasks(ImmutableList.of(tspec));

        Job job = launch(spec);
        return job;
    }

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
        jobDao.nextId(spec);
        createLogPath(spec);

        /**
         * Some basic env vars.
         */
        spec.putToEnv("ZORROA_JOB_ID", String.valueOf(spec.getJobId()));
        spec.putToEnv("ZORROA_JOB_TYPE", spec.getType().toString());

        /**
         * These options allow jobs to talk back to the archivist.
         */
        spec.putToEnv("ZORROA_USER", SecurityUtils.getUsername());
        spec.putToEnv("ZORROA_HMAC_KEY", userDao.getHmacKey(SecurityUtils.getUsername()));

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

    /**
     * Decorates a base path with the final leaf directories
     * for a job.
     *
     * @param spec
     * @param basePath
     * @return
     */
    private Path resolveLogPath(JobSpec spec, Path basePath) {
        return basePath
                .resolve(spec.getType().toString().toLowerCase())
                .resolve(String.valueOf(spec.getJobId()));

    }

    private void createLogPath(JobSpec spec) {

        // The local path is the one archivist can see.
        Path localPath = resolveLogPath(spec,
                properties.getPath("archivist.path.logs"));

        File localFile = localPath.toFile();
        if (localFile.exists()) {
            logger.warn("Log file path exists: {}", localFile);
        }
        else {
            localPath.toFile().mkdirs();
        }

        // The cluster path is what
        Path clusterPath = resolveLogPath(spec,
                properties.getPath("zorroa.cluster.path.logs"));
        spec.setLogPath(clusterPath.toString());
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
        jobDao.incrementWaitingTaskCount(task);
        taskDao.incrementDependCount(task);
        return task;
    }

    @Override
    public Job get(int id) {
        return jobDao.get(id);
    }

    @Override
    public boolean setJobState(JobId job, JobState newState, JobState oldState) {
        boolean result = jobDao.setState(job, newState, oldState);
        if (result) {
            event.afterCommit(()->
                    message.broadcast(new Message("JOB_CANCELED",
                            ImmutableMap.of("id", job.getJobId()))));
        }
        return result;
    }

    @Override
    public boolean setTaskState(TaskId task, TaskState newState) {
        return setTaskState(task, newState, null);
    }

    @Override
    public boolean setTaskState(TaskId task, TaskState newState, TaskState ... expect) {
        Preconditions.checkNotNull(task.getTaskId());
        Preconditions.checkNotNull(task.getJobId());
        Preconditions.checkNotNull(newState);

        /**
         * This locks the task.
         */
        TaskState oldState = taskDao.getState(task, true);

        if (oldState.equals(newState)) {
            return false;
        }

        if (taskDao.setState(task, newState, expect)) {
            jobDao.updateTaskStateCounts(task, newState, oldState);
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
    public boolean setTaskQueued(TaskId script, String host) {
        if (setTaskState(script, TaskState.Queued, TaskState.Waiting)) {
            taskDao.setHost(script, host);
            return true;
        }
        return false;
    }

    @Override
    public boolean setTaskCompleted(ExecuteTaskStopped result) {
        TaskState newState = result.getNewState();
        if (setTaskState(result, newState, TaskState.Running, TaskState.Queued)) {
            if (newState.equals(TaskState.Success)) {
                logger.info("decremented {} depend counts" , taskDao.decrementDependCount(result));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean incrementJobStats(int id, int success, int errors, int warnings) {
        return jobDao.incrementStats(id, success, errors, warnings);
    }

    @Override
    public boolean incrementTaskStats(int id, int success, int errors, int warnings) {
        return taskDao.incrementStats(id, success, errors, warnings);
    }

    @Override
    public PagedList<Job> getAll(Pager page, JobFilter filter) {
        return jobDao.getAll(page, filter);
    }

    @Override
    public PagedList<Task> getAllTasks(int job, Pager page) {
        return taskDao.getAll(job, page);
    }

    @Override
    public int updatePingTime(List<Integer> taskIds) {
        return taskDao.updatePingTime(taskIds);
    }
}
