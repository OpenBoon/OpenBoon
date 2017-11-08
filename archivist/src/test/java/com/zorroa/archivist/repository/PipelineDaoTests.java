package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/9/16.
 */
public class PipelineDaoTests extends AbstractTest {

    @Autowired
    PipelineDao pipelineDao;

    Pipeline pipeline;
    PipelineSpecV spec;

    @Before
    public void init() {
        spec = new PipelineSpecV();
        spec.setType(PipelineType.Import);
        spec.setProcessors(Lists.newArrayList());
        spec.setName("Zorroa Test");
        spec.setDescription("A test pipeline");
        spec.setStandard(true);
        pipeline = pipelineDao.create(spec);
    }

    @Test
    public void testCreate() {
        assertEquals(spec.getType(), pipeline.getType());
        assertEquals(spec.getProcessors(), pipeline.getProcessors());
        assertEquals(spec.getName(), pipeline.getName());
    }

    @Test
    public void testDelete() {
        // cant' delete the standard
        assertFalse(pipelineService.delete(pipeline.getId()));
        // Add new standard
        PipelineSpecV spec = new PipelineSpecV();
        spec.setType(PipelineType.Import);
        spec.setProcessors(Lists.newArrayList());
        spec.setName("ZorroaStandard");
        spec.setDescription("A test pipeline");
        spec.setStandard(true);
        pipelineDao.create(spec);
        assertTrue(pipelineService.delete(pipeline.getId()));
        assertFalse(pipelineService.delete(pipeline.getId()));
    }

    @Test
    public void testUpdate() {
        // Note, you cant change a pipeline type.
        Pipeline update = new Pipeline();
        update.setName("foo");
        update.setDescription("foo bar");
        update.setProcessors(Lists.newArrayList(new ProcessorRef().setClassName("bar.Bing")));
        assertTrue(pipelineDao.update(pipeline.getId(), update));

        pipeline = pipelineDao.refresh(pipeline);
        assertEquals(update.getProcessors(), pipeline.getProcessors());
        assertEquals(update.getName(), pipeline.getName());
    }

    @Test
    public void testUpdateVersionUp() {
        Pipeline update = new Pipeline();
        update.setName("foo");
        update.setDescription("foo bar");
        update.setType(PipelineType.Batch);
        update.setProcessors(Lists.newArrayList(new ProcessorRef().setClassName("bar.Bing")));
        update.setVersionUp(true);

        assertTrue(pipelineDao.update(pipeline.getId(), update));

        pipeline = pipelineDao.refresh(pipeline);
        assertEquals(2, pipeline.getVersion());

        assertTrue(pipelineDao.update(pipeline.getId(), update));
        pipeline = pipelineDao.refresh(pipeline);
        assertEquals(3, pipeline.getVersion());
    }

    @Test
    public void testGet() {
        assertEquals(pipeline, pipelineDao.get(pipeline.getId()));
        assertEquals(pipeline, pipelineDao.get(pipeline.getName()));
    }

    @Test
    public void testGetAllPaged() {
        long count = pipelineDao.count();
        for (int i=0; i<10; i++) {
            spec.setName("Pipeline" + i);
            pipelineDao.create(spec);
        }
        PagedList<Pipeline> list = pipelineDao.getAll(Pager.first(5));

        assertEquals(5, list.getList().size());
        assertEquals(count + 10, (long) list.getPage().getTotalCount());
        assertEquals(3, list.getPage().getTotalPages());
    }

    @Test
    public void testGetAll() {
        long count = pipelineDao.count();
        for (int i=0; i<10; i++) {
            spec.setName("Pipeline" + i);
            pipelineDao.create(spec);
        }
        assertEquals(count+10, pipelineDao.getAll().size());
    }

    @Test
    public void testRefresh() {
        assertEquals(pipeline, pipelineDao.refresh(pipeline));
    }

    @Test
    public void testGetStandard() {
        Pipeline p = pipelineDao.getStandard();
        assertEquals("Zorroa Test", p.getName());
    }
}
