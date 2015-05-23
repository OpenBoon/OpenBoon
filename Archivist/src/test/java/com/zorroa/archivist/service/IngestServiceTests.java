package com.zorroa.archivist.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Asset;
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
    public void testStandardIngest() throws InterruptedException {

         IngestPipeline pipeline = ingestService.getIngestPipeline("standard");
         ingestService.ingest(pipeline, new IngestBuilder(getStaticImagePath()));

         Thread.sleep(1000);
         refreshIndex();


         assertEquals(2, assetDao.getAll().size());
         for (Asset asset: assetDao.getAll()) {
             logger.info("{}", asset.getData());
         }

    }

    @Test
    public void testIngest() throws InterruptedException {

         IngestPipelineBuilder builder = new IngestPipelineBuilder();
         builder.setId("default");
         builder.addToProcessors(new IngestProcessorFactory("com.zorroa.archivist.ingest.ImageMetadataProcessor"));

         IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
         ingestService.ingest(pipeline, new IngestBuilder(getStaticImagePath()));

         Thread.sleep(1000);
         refreshIndex();

         assertEquals(2, assetDao.getAll().size());

         for (Asset asset: assetDao.getAll()) {
             logger.info("{}", asset.getData());
         }

    }
}
