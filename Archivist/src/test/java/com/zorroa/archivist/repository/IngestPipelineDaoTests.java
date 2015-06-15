package com.zorroa.archivist.repository;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.common.collect.Maps;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;

public class IngestPipelineDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Test
    public void getAndCreate() {

        IngestPipelineBuilder request = new IngestPipelineBuilder();
        request.setName("default");

        IngestProcessorFactory processor = new IngestProcessorFactory();
        processor.setKlass("com.zorroa.archivist.ingest.ExifProcessor");
        request.setProcessors(Lists.newArrayList(processor));

        IngestPipeline pipeline = ingestPipelineDao.create(request);

        assertEquals(request.getName(), pipeline.getName());
    }

    @Test
    public void getAll() {

        for (int i=0; i<=10; i++) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("default_" + i);
            builder.addToProcessors(
                    new IngestProcessorFactory("foo.bar.Bing",
                            Maps.newHashMap()));
            ingestPipelineDao.create(builder);
        }
        assertEquals(12, ingestPipelineDao.getAll().size());
    }
}
