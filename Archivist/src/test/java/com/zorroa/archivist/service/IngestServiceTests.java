package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.repository.AssetDao;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngestServiceTests extends ArchivistApplicationTests {

    @Autowired
    IngestService ingestService;

    @Autowired
    ImageService imageService;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testIngest_Standard() throws InterruptedException {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestSchedulerService.executeIngest(ingest);

        refreshIndex(1000);
        assertEquals(2, assetDao.getAll().size());
    }

    @Test
    public void testIngest_Custom() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        builder.addToProcessors(new IngestProcessorFactory(
                "com.zorroa.archivist.processors.AssetMetadataProcessor"));
        ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipeline("default"));

        ingestSchedulerService.executeIngest(ingest);
        refreshIndex(1000);
        assertEquals(2, assetDao.getAll().size());
    }

    @Test
    public void testUpdateIngest() throws InterruptedException {

        IngestPipelineBuilder ipb = new IngestPipelineBuilder();
        ipb.setName("test");
        ipb.setDescription("A test pipeline");
        ipb.addToProcessors(new IngestProcessorFactory("com.zorroa.archivist.processors.ChecksumProcessor"));
        IngestPipeline testPipeline = ingestService.createIngestPipeline(ipb);

        ProxyConfigBuilder pcb = new ProxyConfigBuilder();
        pcb.setName("test");
        pcb.setDescription("test proxy config.");
        pcb.setOutputs(org.elasticsearch.common.collect.Lists.newArrayList(
                new ProxyOutput("png", 128, 8)
        ));
        ProxyConfig testProxyConfig  = imageService.createProxyConfig(pcb);

        IngestUpdateBuilder updateBuilder = new IngestUpdateBuilder();
        updateBuilder.setPipeline(testPipeline.getName());
        updateBuilder.setProxyConfig(testProxyConfig.getName());

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        assertTrue(ingestService.updateIngest(ingest, updateBuilder));

        ingest = ingestService.getIngest(ingest.getId());
        assertEquals(testPipeline.getId(), ingest.getPipelineId());
        assertEquals(testProxyConfig.getId(), ingest.getProxyConfigId());
    }
}
