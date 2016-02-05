package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.ingestors.ChecksumProcessor;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngestServiceTests extends ArchivistApplicationTests {

    @Autowired
    AssetDao assetDao;

    @Test
    public void testIngest_Standard() throws InterruptedException {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        ingestExecutorService.executeIngest(ingest);

        refreshIndex();
        assertEquals(2, assetDao.getAll().size());
    }

    @Test
    public void testIngest_Custom() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipelineId(pipeline.getId()));

        ingestExecutorService.executeIngest(ingest);
        refreshIndex();
        assertEquals(2, assetDao.getAll().size());
    }

    @Test
    public void testUpdateIngest() throws InterruptedException {

        IngestPipelineBuilder ipb = new IngestPipelineBuilder();
        ipb.setName("test");
        ipb.setDescription("A test pipeline");
        ipb.addToProcessors(new ProcessorFactory<>(ChecksumProcessor.class));
        IngestPipeline testPipeline = ingestService.createIngestPipeline(ipb);

        IngestUpdateBuilder updateBuilder = new IngestUpdateBuilder();
        updateBuilder.setPipelineId(testPipeline.getId());

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        assertTrue(ingestService.updateIngest(ingest, updateBuilder));

        ingest = ingestService.getIngest(ingest.getId());
        assertEquals(testPipeline.getId(), ingest.getPipelineId());
    }


    @Test(expected=EmptyResultDataAccessException.class)
    public void testDelete() throws InterruptedException {

        IngestPipelineBuilder ipb = new IngestPipelineBuilder();
        ipb.setName("test");
        ipb.setDescription("A test pipeline");
        ipb.addToProcessors(new ProcessorFactory<>(ChecksumProcessor.class));
        IngestPipeline testPipeline = ingestService.createIngestPipeline(ipb);

        IngestUpdateBuilder updateBuilder = new IngestUpdateBuilder();
        updateBuilder.setPipelineId(testPipeline.getId());

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        assertTrue(ingestService.deleteIngest(ingest));

        ingestService.getFolder(ingest);
    }
}
