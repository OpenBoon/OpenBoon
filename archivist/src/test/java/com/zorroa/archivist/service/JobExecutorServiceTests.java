package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Job;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.sdk.processor.ProcessorSpec;
import com.zorroa.sdk.zps.ZpsReaction;
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

        ZpsReaction react = ZpsReaction.react(script);
        react.expand(new ZpsScript().setPipeline(ImmutableList.of(new ProcessorSpec()
                .setClassName("foo")
                .setLanguage("java")
                .setPlugin("zorroa-core"))));

        jobExecutorService.react(react);
        job = jobService.get(zps.getJobId());
        assertEquals(1, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(1, job.getCounts().getTasksWaiting());
        assertEquals(2, job.getCounts().getTasksTotal());
        assertEquals(0, job.getCounts().getTasksCompleted());

    }
}
