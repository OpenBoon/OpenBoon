package com.zorroa.archivist.service;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.TestIngestor;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.processor.ProcessorFactory;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngestServiceTests extends AbstractTest {

    @Test
    public void testResetRunningIngests() throws InterruptedException {
        List<Ingest> ingests = Lists.newArrayList();
        for (int i=0; i<6; i++)  {
            ingests.add(ingestService.createIngest(new IngestBuilder(getStaticImagePath())));
        }

        assertTrue(ingestService.setIngestPaused(ingests.get(0)));
        assertTrue(ingestService.setIngestQueued(ingests.get(1)));
        assertTrue(ingestService.setIngestRunning(ingests.get(2)));

        long resets = ingestService.resetRunningIngests();
        assertEquals(3, resets);
    }

    @Test
    public void testUpdateIngest() throws InterruptedException {

        IngestPipelineBuilder ipb = new IngestPipelineBuilder();
        ipb.setName("test");
        ipb.setDescription("A test pipeline");
        ipb.addToProcessors(new ProcessorFactory<>(TestIngestor.class));
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
        ipb.addToProcessors(new ProcessorFactory<>(TestIngestor.class));
        IngestPipeline testPipeline = ingestService.createIngestPipeline(ipb);

        IngestUpdateBuilder updateBuilder = new IngestUpdateBuilder();
        updateBuilder.setPipelineId(testPipeline.getId());

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()));
        assertTrue(ingestService.deleteIngest(ingest));

        ingestService.getFolder(ingest);
    }
}
