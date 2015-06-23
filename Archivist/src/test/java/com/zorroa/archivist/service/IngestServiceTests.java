package com.zorroa.archivist.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.repository.AssetDao;

public class IngestServiceTests extends ArchivistApplicationTests {

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestSchedulerService ingestSchedulerService;

    @Autowired
    AssetDao assetDao;

    @Test
    public void testIngest_Standard() throws InterruptedException {

        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        Ingest ingest01 = ingestSchedulerService.executeNextIngest();

        refreshIndex(1000);
        assertEquals(2, assetDao.getAll().size());

        // Ingest again!
        ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        Ingest ingest02 = ingestSchedulerService.executeNextIngest();
        refreshIndex(1000);

        assertEquals(2, ingestService.getIngest(ingest01.getId()).getCreatedCount());
        assertEquals(0, ingestService.getIngest(ingest01.getId()).getErrorCount());
        assertEquals(0, ingestService.getIngest(ingest02.getId()).getCreatedCount());
        assertEquals(0, ingestService.getIngest(ingest02.getId()).getErrorCount());
    }

    @Test
    public void testIngest_Custom() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        builder.addToProcessors(new IngestProcessorFactory(
                "com.zorroa.archivist.processors.AssetMetadataProcessor"));
        ingestService.createIngestPipeline(builder);
        ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipeline("default"));

        ingestSchedulerService.executeNextIngest();
        refreshIndex(1000);
        assertEquals(2, assetDao.getAll().size());
    }
}
