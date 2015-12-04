package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.processors.ChecksumProcessor;
import com.zorroa.archivist.processors.ProxyProcessor;
import com.zorroa.archivist.sdk.domain.IngestPipeline;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestPipelineUpdateBuilder;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import org.elasticsearch.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

public class IngestPipelineDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    IngestPipeline pipeline;

    @Before
    public void init() {
        IngestPipelineBuilder request = new IngestPipelineBuilder();
        request.setName("test");
        request.setDescription("a test pipeline");
        request.setProcessors(Lists.newArrayList(new ProcessorFactory<>(ChecksumProcessor.class)));
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

        for (int i = 0; i <= 10; i++) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("default_" + i);
            builder.addToProcessors(
                    new ProcessorFactory<>("foo.bar.Bing",
                            Maps.newHashMap()));
            ingestPipelineDao.create(builder);
        }
        assertEquals(13, ingestPipelineDao.getAll().size());
    }

    @Test
    public void update() throws InterruptedException {
        // Sleep before doing the update to ensure the modified
        // time property is incremented.
        Thread.sleep(5);

        IngestPipelineUpdateBuilder builder = new IngestPipelineUpdateBuilder();
        builder.setName("foo");
        builder.setDescription("foo");
        builder.setProcessors(Lists.newArrayList(new ProcessorFactory<>(ProxyProcessor.class)));

        assertTrue(ingestPipelineDao.update(pipeline, builder));
        IngestPipeline updated = ingestPipelineDao.get(pipeline.getId());
        assertEquals(builder.getDescription(), updated.getDescription());
        assertEquals(builder.getName(), updated.getName());
        assertEquals(builder.getProcessors(), updated.getProcessors());
        assertNotEquals(pipeline.getTimeModified(), updated.getTimeModified());
    }

    @Test
    public void delete() {
        assertTrue(ingestPipelineDao.delete(pipeline));
        assertFalse(ingestPipelineDao.delete(pipeline));
    }
}
