package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/12/16.
 */
public class JobExecutorServiceTests extends AbstractTest {

    @Autowired
    JobExecutorService jobExecutorService;

    @Autowired
    JobService jobService;

    @Test
    public void testEndToEndScheduleWithExpand() {

        ZpsScript script = new ZpsScript();
        script.setName("foo-bar");

        ZpsScript zps = jobService.launch(script, PipelineType.Import);
        Job job = jobService.get(zps.getJobId());
        assertEquals(0, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(1, job.getCounts().getTasksWaiting());
        jobExecutorService.schedule();

        job = jobService.get(zps.getJobId());
        assertEquals(1, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(0, job.getCounts().getTasksWaiting());

        ZpsScript expand = ZpsScript.copy(script);
        expand.setExecute(ImmutableList.of(new ProcessorRef()
                .setClassName("foo")
                .setLanguage("java")));

        jobService.expand(expand);
        job = jobService.get(zps.getJobId());
        assertEquals(1, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(1, job.getCounts().getTasksWaiting());
        assertEquals(2, job.getCounts().getTasksTotal());
        assertEquals(0, job.getCounts().getTasksCompleted());
    }

    @Test
    public void testDependency() {

        ZpsScript script = new ZpsScript();
        script.setName("foo-bar");

        ZpsScript job = jobService.launch(script, PipelineType.Import);
        ZpsScript task = jobService.createTask(ZpsScript.copy(job));
        jobService.createParentDepend(task);
        jobService.setTaskState(job, TaskState.Running, TaskState.Waiting);

        int dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, dependCount);

        jobService.setTaskCompleted(job, 0);
        dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(0, dependCount);
    }

    @Test
    public void testDependencyWithExpand() {

        ZpsScript script = new ZpsScript();
        script.setName("foo-bar");

        ZpsScript job = jobService.launch(script, PipelineType.Import);
        ZpsScript task = jobService.createTask(ZpsScript.copy(job));
        jobService.createParentDepend(task);
        ZpsScript anotherTask = jobService.createTask(ZpsScript.copy(job));

        jobService.setTaskState(job, TaskState.Running, TaskState.Waiting);

        int dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(2, dependCount);

        jobService.setTaskCompleted(job, 0);
        dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(1, dependCount);

        jobService.setTaskState(anotherTask, TaskState.Running, TaskState.Waiting);
        jobService.setTaskCompleted(anotherTask, 0);

        dependCount = jdbc.queryForObject(
                "SELECT SUM(int_depend_count) FROM task WHERE pk_job=?", Integer.class, job.getJobId());
        assertEquals(0, dependCount);
    }
}
