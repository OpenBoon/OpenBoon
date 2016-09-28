package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 8/17/16.
 */
public class PipelineServiceTests extends AbstractTest {

    @Autowired
    PipelineService pipelineService;

    Pipeline pipeline;
    PipelineSpecV spec;

    @Before
    public void init() {
        spec = new PipelineSpecV();
        spec.setProcessors(Lists.newArrayList(
                new ProcessorRef("com.zorroa.sdk.processor.builtin.GroupProcessor")));
        spec.setDescription("A NoOp");
        spec.setName("test");
        spec.setType(PipelineType.Import);
        pipeline = pipelineService.create(spec);
    }

    @Test
    public void testGet() {
        Pipeline p1 = pipelineService.get(pipeline.getId());
        Pipeline p2 = pipelineService.get(pipeline.getName());
        assertEquals(p1, p2);
        assertEquals(p1, pipeline);
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testDelete() {
        assertTrue(pipelineService.delete(pipeline.getId()));
        pipelineService.get(pipeline.getId());
    }

    @Test
    public void testGetAll() {
        assertEquals(1, pipelineService.getAll().size());
    }

    @Test
    public void testExists() {
        assertTrue(pipelineService.exists("test"));
        assertFalse(pipelineService.exists("false"));
    }

    @Test
    public void testUpdate() {
        pipeline.setName("foo");
        pipeline.setDescription("bar");
        pipeline.setType(PipelineType.Export);
        pipeline.setProcessors(Lists.newArrayList());
        assertTrue(pipelineService.update(pipeline.getId(), pipeline));

        Pipeline p = pipelineService.get(pipeline.getId());
        assertEquals(pipeline.getName(), p.getName());
        assertEquals(pipeline.getDescription(), p.getDescription());
        assertEquals(pipeline.getType(), p.getType());
        assertEquals(pipeline.getProcessors(), p.getProcessors());
    }
}
