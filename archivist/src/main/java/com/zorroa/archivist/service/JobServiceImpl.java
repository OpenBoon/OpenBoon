package com.zorroa.archivist.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.JobDao;
import com.zorroa.archivist.repository.PipelineDao;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.cluster.thrift.ExpandT;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.config.NetworkEnvironment;
import com.zorroa.archivist.domain.JobId;
import com.zorroa.archivist.domain.TaskId;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.client.exception.ArchivistException;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    UserDao userDao;

    @Autowired
    PipelineDao pipelineDao;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    PluginService pluginService;

    @Autowired
    NetworkEnvironment networkEnv;

    /**
     * Launch a validated JobSpec provided by REST endpoint.
     * @param specv
     * @return
     */
    @Override
    public Job launch(JobSpecV specv) {

        if (!JdbcUtils.isValid(specv.getScript().getGenerate()) &&
               !JdbcUtils.isValid(specv.getScript().getOver())) {
            // Add 1 empty frame.
            specv.getScript().setOver(Lists.newArrayList(new Document()));
            //throw new IllegalArgumentException("Script has neither data to iterate over or a generator");
        }

        if (!JdbcUtils.isValid(specv.getScript().getExecute())) {
            //throw new IllegalArgumentException("Script has no execute clause.");
            specv.getScript().setExecute(Lists.newArrayList());
        }

        specv.getScript().setExecute(
                pluginService.getProcessorRefs(specv.getScript().getExecute()));
        specv.getScript().setGenerate(
                pluginService.getProcessorRefs(specv.getScript().getGenerate()));

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
        Path rootPath = createSharedPaths(spec);

        /**
         * Some basic env vars.
         */
        spec.putToEnv("ZORROA_JOB_ID", String.valueOf(spec.getJobId()));
        spec.putToEnv("ZORROA_JOB_TYPE", spec.getType().toString());
        spec.putToEnv("ZORROA_JOB_PATH_ROOT", rootPath.toString());

        for (String dir: CHILD_DIRS) {
            spec.putToEnv("ZORROA_JOB_PATH_" + StringUtils.upperCase(dir),
                    rootPath.resolve(dir).toString());
        }
        /**
         * These options allow jobs to talk back to the archivist.
         */
        spec.putToEnv("ZORROA_ARCHIVIST_URL", networkEnv.getPrivateUri().toString());
        spec.putToEnv("ZORROA_USER", SecurityUtils.getUsername());
        spec.putToEnv("ZORROA_HMAC_KEY", userDao.getHmacKey(SecurityUtils.getUsername()));

        Job job = jobDao.create(spec);
        if (spec.getTasks() != null) {
            for (TaskSpec tspec: spec.getTasks()) {
                createTask(tspec.setJobId(job.getJobId()));
            }
        }
        return jobDao.get(job.getId());
    }

    @Override
    public boolean createParentDepend(TaskId task) {
        return taskDao.createParentDepend(task);
    }

    @Override
    public Task expand(Task parent, ExpandT expand) {
        if (logger.isDebugEnabled()) {
            logger.debug("Expanding: {}", Json.prettyString(expand));
        }

        ZpsScript expandScript = Json.deserialize(expand.getScript(), ZpsScript.class);

        /*
         * If the expand script is null, inherit the execute from the parent task.
         */
        if (expandScript.getExecute() == null) {
            try {
                ZpsScript parentScript = Json.Mapper.readValue(new File(parent.getScriptPath()), ZpsScript.class);
                expandScript.setExecute(parentScript.getExecute());
            } catch (IOException e) {
                throw new ArchivistWriteException("Expand with inherited execute failure", e);
            }
        }

        TaskSpec spec = new TaskSpec();
        spec.setJobId(parent.getJobId());
        spec.setName(expand.getName());
        spec.setScript(Json.serializeToString(expandScript));
        spec.setOrder(parent.getOrder());
        spec.setParentTaskId(parent.getTaskId());
        return createTask(spec);
    }

    @Override
    public Task createTask(TaskSpecV spec) {

        /**
         * Use the standard pipeline if one is not set.
         */
        if (spec.getPipelineId() == null || spec.getPipelineId() <= 0) {
            Pipeline pl = pipelineDao.getStandard();
            spec.setPipelineId(pl.getId());
        }

        TaskSpec ts = new TaskSpec();
        ts.setName(spec.getName());
        ts.setJobId(spec.getJobId());
        ts.setParentTaskId(null);
        ts.setOrder(Task.ORDER_DEFAULT);

        ZpsScript script = new ZpsScript();
        script.setOver(spec.getDocs());
        script.setExecute(pluginService.getProcessorRefs(spec.getPipelineId()));
        script.getExecute().add( new ProcessorRef()
                .setClassName("com.zorroa.core.collector.IndexDocumentCollector")
                .setLanguage("java")
                .setArgs(ImmutableMap.of("importId", ts.getJobId())));

        Task task = taskDao.create(ts);

        /**
         * Write script to disk
         */
        try {
            Json.Mapper.writeValue(new File(task.getScriptPath()), script);
        } catch (IOException e) {
            throw new ArchivistException("Failed to add task, " + e, e);
        }

        jobDao.incrementWaitingTaskCount(task);
        taskDao.incrementDependCount(task);
        return task;
    }

    public Task createTask(TaskSpec spec) {

        /**
         * Create the first task which is just the script itself.
         */
        Task task = taskDao.create(spec);
        try {
            Files.write(Paths.get(task.getScriptPath()), spec.getScript().getBytes());
        } catch (IOException e) {
            throw new ArchivistException("Failed to add task, " + e, e);
        }

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
    public boolean setTaskCompleted(Task task, int exitStatus) {
        logger.info("Task {} [{}] completed on host '{}' exit status: {}", task.getName(),
            task.getId(), task.getHost(), exitStatus);
        TaskState newState = exitStatus != 0 ? TaskState.Failure : TaskState.Success;
        if (setTaskState(task, newState, TaskState.Running, TaskState.Queued)) {
            if (newState.equals(TaskState.Success)) {
                taskDao.decrementDependCount(task);
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

    /**
     * Takes jobspec with a populated job Id and create a path for the job
     * to store its data, including logs.
     *
     * @param spec
     * @return
     */
    @Override
    public Path resolveJobRoot(JobSpec spec) {
        Path basePath = properties.getPath("zorroa.cluster.path.jobs");

        DateTime time = new DateTime();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY/MM/dd");

        return basePath
                .resolve(spec.getType().toString().toLowerCase())
                .resolve(formatter.print(time))
                .resolve(SecurityUtils.getUsername())
                .resolve(String.valueOf(spec.getJobId()))
                .toAbsolutePath();
    }

    /**
     * The subdirectories made in a job directory.
     *
     * logs and tmp get removed by maintenance.
     * if assets has files its left alone
     */
    private String[] CHILD_DIRS = new String[] {
        "logs",
        "tmp",
        "assets",
        "exported",
        "scripts"
    };

    private Path createSharedPaths(JobSpec spec) {
        Path rootPath = resolveJobRoot(spec);
        logger.info("creating shared paths: {}", rootPath);
        for (String child: CHILD_DIRS) {
            Path childPath = rootPath.resolve(child);

            File childFile = childPath.toFile();
            if (childFile.exists()) {
                logger.warn("Log file path exists: {}", childFile);
            } else {
                childFile.mkdirs();
            }
        }
        spec.setRootPath(rootPath.toString());
        return rootPath;
    }
}
