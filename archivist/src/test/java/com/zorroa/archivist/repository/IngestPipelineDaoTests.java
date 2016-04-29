package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.TestAggregator;
import com.zorroa.archivist.TestIngestor;
import com.zorroa.archivist.sdk.domain.IngestPipeline;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestPipelineUpdateBuilder;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class IngestPipelineDaoTests extends AbstractTest {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    IngestPipeline pipeline;

    @Before
    public void init() {
        IngestPipelineBuilder request = new IngestPipelineBuilder();
        request.setName("test");
        request.setDescription("a test pipeline");
        request.setProcessors(Lists.newArrayList(new ProcessorFactory<>(TestIngestor.class)));
        pipeline = ingestPipelineDao.create(request);
    }

    @Test
    public void getAndCreate() {
        IngestPipeline _pipeline = ingestPipelineDao.get(pipeline.getId());
        assertEquals(_pipeline.getName(), pipeline.getName());
        assertEquals(_pipeline.getDescription(), pipeline.getDescription());
    }

    @Test
    public void getAll() {
        int count = ingestPipelineDao.getAll().size();

        for (int i = 0; i <= 10; i++) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("default_" + i);
            builder.addToProcessors(
                    new ProcessorFactory<>("foo.bar.Bing",
                            Maps.newHashMap()));
            ingestPipelineDao.create(builder);
        }
        assertEquals(count + 11, ingestPipelineDao.getAll().size());
    }

    @Test
    public void update() throws InterruptedException {
        // Sleep before doing the update to ensure the modified
        // time property is incremented.
        Thread.sleep(5);

        IngestPipelineUpdateBuilder builder = new IngestPipelineUpdateBuilder();
        builder.setName("foo");
        builder.setDescription("foo");
        builder.setProcessors(Lists.newArrayList(new ProcessorFactory<>(TestIngestor.class)));
        builder.setAggregators(Lists.newArrayList(new ProcessorFactory<>(TestAggregator.class)));
        assertTrue(ingestPipelineDao.update(pipeline, builder));
        IngestPipeline updated = ingestPipelineDao.get(pipeline.getId());
        assertEquals(builder.getDescription(), updated.getDescription());
        assertEquals(builder.getName(), updated.getName());
        assertEquals(builder.getProcessors(), updated.getProcessors());
        assertEquals(builder.getAggregators(), updated.getAggregators());
        assertNotEquals(pipeline.getTimeModified(), updated.getTimeModified());
    }

    @Test
    public void delete() {
        assertTrue(ingestPipelineDao.delete(pipeline));
        assertFalse(ingestPipelineDao.delete(pipeline));
    }
}
