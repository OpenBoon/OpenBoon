package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.domain.TaskState;
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

    ZpsScript script;

    @Before
    public void init() {
        script = new ZpsScript();
        script.setName("foo-bar");
        jobService.launch(script, PipelineType.Import);
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
        assertTrue(jobService.setTaskQueued(script));
        assertTrue(jobService.setTaskState(script, TaskState.Running, TaskState.Queued));
        assertTrue(jobService.setTaskCompleted(script, 1));
        assertEquals(JobState.Finished, jobService.get(script.getJobId()).getState());
    }
}
