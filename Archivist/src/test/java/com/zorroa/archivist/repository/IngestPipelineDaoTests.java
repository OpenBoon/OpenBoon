package com.zorroa.archivist.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorWrapper;

public class IngestPipelineDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Test
    public void getAndCreate() {

        IngestPipelineBuilder request = new IngestPipelineBuilder();
        request.setName("default");

        IngestProcessorWrapper processor = new IngestProcessorWrapper();
        processor.setKlass("com.zorroa.archivist.ingest.ExifProcessor");
        request.setProcessors(Lists.newArrayList(processor));

        String id = ingestPipelineDao.create(request);
        IngestPipeline pipeline = ingestPipelineDao.get(id);

        assertEquals(request.getName(), pipeline.getName());
    }

}
