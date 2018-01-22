package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobSpec;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.TaskSpec;
import com.zorroa.archivist.repository.TaskDao;
import com.zorroa.cluster.thrift.ExpandT;
import com.zorroa.cluster.thrift.TaskStartT;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.Expand;
import com.zorroa.sdk.processor.ExpandFrame;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/12/16.
 */
public class JobExecutorServiceTests extends AbstractTest {

    @Autowired
    JobExecutorService jobExecutorService;

    @Autowired
    JobService jobService;

    @Autowired
    TaskDao taskDao;

    @Test
    public void testEndToEndScheduleWithExpand() {

        JobSpec spec = new JobSpec();
        spec.setName("foo-bar");
        spec.setType(PipelineType.Import);

        TaskSpec tspec = new TaskSpec();
        tspec.setScript(new ZpsScript());
        tspec.setName("a task");
        spec.setTasks(Lists.newArrayList(tspec));

        Job job = jobService.launch(spec);
        assertEquals(0, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(1, job.getCounts().getTasksWaiting());

        unittestSchedule();

        job = jobService.get(job.getJobId());
        assertEquals(1, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(0, job.getCounts().getTasksWaiting());

        Expand expand = new Expand();
        expand.setExecute(ImmutableList.of(new ProcessorRef()
                .setClassName("foo")
                .setLanguage("java")));
        expand.setFrames(ImmutableList.of(new ExpandFrame(new Document())));

        jobService.expand(jobService.getAllTasks(job.getJobId(), Pager.first()).get(0), new ExpandT()
                .setName("foo")
                .setScript(Json.serializeToString(expand).getBytes()));

        job = jobService.get(job.getJobId());
        assertEquals(1, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(1, job.getCounts().getTasksWaiting());
        assertEquals(2, job.getCounts().getTasksTotal());
        assertEquals(0, job.getCounts().getTasksCompleted());
    }

    @Test
    public void testDependency() {

        JobSpec spec = new JobSpec();
        spec.setName("foo-bar");
        spec.setType(PipelineType.Import);

        Job job = jobService.launch(spec);
        Task task1 = jobService.createTask(
                new TaskSpec(job.getJobId(), "task1")
                        .setScript(new ZpsScript()));
        Task task2 = jobService.createTask(
                new TaskSpec(job.getJobId(), "task2")
                        .setScript(new ZpsScript())
                        .setParentTaskId(task1.getTaskId()));

        jobService.createParentDepend(task2);
        jobService.setTaskState(task1, TaskState.Running, TaskState.Waiting);

        int dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, dependCount);

        jobService.setTaskCompleted(task1, 0);

        dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(0, dependCount);
    }

    @Test
    public void testDependencyWithExpand() {

        JobSpec spec = new JobSpec();
        spec.setName("foo-bar");
        spec.setType(PipelineType.Import);
        Job job = jobService.launch(spec);

        Task task1 = jobService.createTask(
                new TaskSpec(job.getJobId(), "task1")
                        .setScript(new ZpsScript()));
        Task task2 = jobService.createTask(
                new TaskSpec(job.getJobId(), "task2")
                        .setParentTaskId(task1.getTaskId())
                        .setScript(new ZpsScript()));

        jobService.createParentDepend(task2);

        // Make an expand from the original parent task.
        Task task3 = jobService.createTask(
                new TaskSpec(job.getJobId(), "task3")
                        .setScript(new ZpsScript())
                        .setParentTaskId(task1.getTaskId()));

        jobService.setTaskState(task1, TaskState.Running, TaskState.Waiting);

        int dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(2, dependCount);

        jobService.setTaskCompleted(task1, 0);

        dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, dependCount);

        jobService.setTaskState(task3, TaskState.Running, TaskState.Waiting);

        jobService.setTaskCompleted(task3, 0);

        dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(0, dependCount);
    }

    @Test
    public void cancelAndRestart() {
        JobSpec spec = new JobSpec();
        spec.setName("foo-bar");
        spec.setType(PipelineType.Import);

        TaskSpec tspec = new TaskSpec();
        tspec.setScript(new ZpsScript());
        tspec.setName("a task");
        spec.setTasks(Lists.newArrayList(tspec));

        Job job = jobService.launch(spec);

        assertTrue(jobExecutorService.cancelJob(job));
        assertFalse(jobExecutorService.cancelJob(job));
        assertTrue(jobExecutorService.restartJob(job));
        assertFalse(jobExecutorService.restartJob(job));
    }

    /**
     * Called by unit tests.
     */
    public void unittestSchedule() {
        for (TaskStartT task: taskDao.getWaiting(10)) {
            logger.debug("SCHEDULE");
            logger.debug("{}", Json.prettyString(task));
            logger.debug("SCHEDULE");

            Task t = taskDao.get(task.id);
            if (!jobService.setTaskState(t, TaskState.Queued, TaskState.Waiting)) {
                throw new RuntimeException("Failed to queue task");
            }

            if (!jobService.setTaskState(t, TaskState.Running, TaskState.Queued)) {
                logger.warn("Failed to set task running: {}", task);
                throw new RuntimeException("Failed to run task");
            }
        }
    }

}
