package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.Document;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.zps.ZpsScript;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/13/16.
 */
public class JobServiceTests extends AbstractTest {

    @Autowired
    JobService jobService;

    JobSpec spec;
    Job job;
    TaskSpec tspec;
    Task task;

    @Before
    public void init() {
        JobSpec spec = new JobSpec();
        spec.setName("foo");
        spec.setType(PipelineType.Export);
        job = jobService.launch(spec);

        tspec = new TaskSpec();
        tspec.setName("task1");
        tspec.setScript(new ZpsScript());
        tspec.setJobId(job.getJobId());
        task = jobService.createTask(tspec);

        job = jobService.get(job.getId());
    }

    @Test
    public void jobProgress() {
        Job job  = new Job();
        Job.Counts c = new Job.Counts();
        c.setTasksCompleted(1);
        c.setTasksTotal(10);
        c.setTasksFailure(1);
        c.setTasksQueued(1);
        c.setTasksWaiting(5);
        c.setTasksRunning(2);
        c.setTasksSuccess(1);
        job.setCounts(c);

        logger.info("{}", job.getProgress());
    }

    @Test
    public void setTaskCompleted() {
        assertTrue(jobService.setTaskQueued(task));
        assertTrue(jobService.setTaskState(task, TaskState.Running, TaskState.Queued));
        assertTrue(jobService.setTaskCompleted(task, 0));
        assertEquals(JobState.Finished, jobService.get(task.getJobId()).getState());
    }

    @Test
    public void setTaskState() {

        assertEquals(0, job.getCounts().getTasksRunning());
        assertEquals(1, job.getCounts().getTasksWaiting());

        assertTrue(jobService.setTaskState(task, TaskState.Running, null));
        job = jobService.get(job.getId());

        assertEquals("running count not incremented", 1, job.getCounts().getTasksRunning());
        assertEquals("waiting count not decremented", 0, job.getCounts().getTasksWaiting());
    }

    @Test
    public void testContinueImportTask() {

        Pipeline p = pipelineService.create(new PipelineSpecV()
                .setDescription("A pipeline")
                .setName("foo")
                .setProcessors(Lists.newArrayList())
                .setType(PipelineType.Import));

        TaskSpecV tsv = new TaskSpecV();
        tsv.setJobId(job.getJobId());
        tsv.setName("A Task");
        tsv.setDocs(Lists.newArrayList(new Document()));
        tsv.setPipelineId(p.getId());

        Task task = jobService.continueImportTask(tsv);
        assertEquals(job.getId(), job.getId());
        assertEquals(2, jobService.getAllTasks(job.getJobId(), Pager.first()).size());
    }
}
