package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.processor.ProcessorRef;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/12/16.
 */

public class ImportServiceTests extends AbstractTest {

    @Autowired
    JobService jobService;

    @Autowired
    ImportService importService;

    @Autowired
    PluginService pluginService;

    Job job;

    @Before
    public void init() {
        pluginService.installAndRegisterAllPlugins();
        ImportSpec spec = new ImportSpec();
        spec.setGenerators(ImmutableList.of(
                new ProcessorRef("com.zorroa.core.common.GroupProcessor")));
        job = importService.create(spec);
    }

    @Before
    public void testCreateMultiplePipelines() throws IOException {

        Pipeline p1 = pipelineService.create(new PipelineSpecV()
                .setName("p1")
                .setProcessors(ImmutableList.of(
                        new ProcessorRef("com.zorroa.core.common.GroupProcessor")))
                .setDescription("p1")
                .setType(PipelineType.Import));

        Pipeline p2 = pipelineService.create(new PipelineSpecV()
                .setName("p2")
                .setProcessors(ImmutableList.of(
                        new ProcessorRef("com.zorroa.core.common.GroupProcessor")))
                .setDescription("p2")
                .setType(PipelineType.Import));

        ImportSpec spec = new ImportSpec();
        spec.setProcessors(ImmutableList.of(
                new ProcessorRef().setPipeline(p1.getId()),
                new ProcessorRef().setPipeline(p2.getName())
        ));
        spec.setName("go go go");
        job = importService.create(spec);
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testCreateFailure() {
        ImportSpec spec = new ImportSpec();
        spec.setGenerators(ImmutableList.of(new ProcessorRef("foo-bar", "java",
                ImmutableMap.of("paths", ImmutableList.of("/tmp/foo.jpg")))));
        job = importService.create(spec);
    }

    @Test
    public void testCreate() {

        job = jobService.get(job.getJobId());
        assertEquals(PipelineType.Import, job.getType());
        assertTrue(job.getTimeStarted() > 0);

        assertEquals(1, job.getCounts().getTasksTotal());
        assertEquals(1, job.getCounts().getTasksWaiting());
        assertEquals(0, job.getCounts().getTasksQueued());
        assertEquals(0, job.getCounts().getTasksRunning());
        assertEquals(0, job.getCounts().getTasksSuccess());
        assertEquals(0, job.getCounts().getTasksFailure());

        assertEquals(0, job.getStats().getAssetTotalCount());
        assertEquals(0, job.getStats().getAssetCreatedCount());
        assertEquals(0, job.getStats().getAssetErrorCount());
        assertEquals(0, job.getStats().getAssetWarningCount());
    }
}
