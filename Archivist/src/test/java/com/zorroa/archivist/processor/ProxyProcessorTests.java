package com.zorroa.archivist.processor;

import java.util.Map;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.service.IngestService;

public class ProxyProcessorTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Test
    public void testProcess() throws InterruptedException {

        Map<String, Object> args = Maps.newHashMap();
        args.put("proxy-sizes", Lists.newArrayList(
                Lists.newArrayList(300, 300)));
        args.put("proxy-scales", Lists.newArrayList(0.25));

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setId("default");
        builder.addToProcessors(
                new IngestProcessorFactory("com.zorroa.archivist.ingest.ProxyProcessor", args));
        ingestPipelineDao.create(builder);

        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        ingestService.ingest(pipeline, new IngestBuilder(getStaticImagePath()));

        Thread.sleep(1000);
        refreshIndex();

        //TODO need callack or API in place to check the ingest works

    }

}
