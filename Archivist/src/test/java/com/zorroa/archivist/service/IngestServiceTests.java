package com.zorroa.archivist.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.repository.AssetDao;

public class IngestServiceTests extends ArchivistApplicationTests {

    @Autowired
    IngestService ingestService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testIngest_Standard() throws InterruptedException {

         IngestPipeline pipeline = ingestService.getIngestPipeline("standard");
         ingestService.ingest(pipeline, new IngestBuilder(getStaticImagePath()));

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
         ingestService.ingest(pipeline, new IngestBuilder(getStaticImagePath()));

         refreshIndex(1000);
         assertEquals(2, assetDao.getAll().size());
    }
}
