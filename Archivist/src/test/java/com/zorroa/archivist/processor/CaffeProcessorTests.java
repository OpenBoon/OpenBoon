package com.zorroa.archivist.processor;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.service.IngestService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CaffeProcessorTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Test
    public void testProcess() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("caffe");
        builder.addToProcessors(new IngestProcessorFactory("com.zorroa.archivist.processors.CaffeProcessor", null));
        ingestPipelineDao.create(builder);

        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));

        Thread.sleep(1000);
        refreshIndex();

        // TODO need callback or API in place to check the ingest works
    }
}
