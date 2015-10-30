package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.ImageService;
import com.zorroa.archivist.sdk.service.IngestService;
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
    IngestExecutorService ingestExecutorService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testIngest_Standard() throws InterruptedException {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);

        refreshIndex(1000);
        assertEquals(2, assetDao.getAll().size());
    }

    @Test
    public void testIngest_Custom() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        builder.addToProcessors(new IngestProcessorFactory(
                "com.zorroa.archivist.processors.AssetMetadataProcessor"));
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipelineId(pipeline.getId()));

        ingestExecutorService.executeIngest(ingest);
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

        IngestUpdateBuilder updateBuilder = new IngestUpdateBuilder();
        updateBuilder.setPipelineId(testPipeline.getId());

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        assertTrue(ingestService.updateIngest(ingest, updateBuilder));

        ingest = ingestService.getIngest(ingest.getId());
        assertEquals(testPipeline.getId(), ingest.getPipelineId());
    }
}
