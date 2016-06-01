package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.TestIngestor;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.exception.ArchivistException;
import com.zorroa.sdk.processor.ProcessorFactory;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IngestServiceTests extends AbstractTest {

    @Test
    public void updateIngestPipeline() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("updateTest");
        builder.setDescription("updateTest");
        builder.setProcessors(ImmutableList.of(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ImageIngestor")));
        IngestPipeline p = ingestService.createIngestPipeline(builder);

        IngestPipelineUpdateBuilder update = new IngestPipelineUpdateBuilder();
        update.setProcessors(ImmutableList.of(new ProcessorFactory<>("com.zorroa.plugins.ingestors.PdfIngestor")));
        update.setDescription("updateTestUpdated");
        update.setName("updateTestUpdated");
        assertTrue(ingestService.updateIngestPipeline(p, update));

        IngestPipeline p2 = ingestService.getIngestPipeline(p.getId());
        assertEquals(update.getProcessors(), p2.getProcessors());
        assertEquals(update.getDescription(), p2.getDescription());
        assertEquals(update.getName(), p2.getName());
    }

    @Test(expected = ArchivistException.class)
    public void updateIngestPipelineInvalidProcessor() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.setDescription("test");
        builder.setProcessors(ImmutableList.of(new ProcessorFactory<>()));
        IngestPipeline p = ingestService.createIngestPipeline(builder);

        IngestPipelineUpdateBuilder update = new IngestPipelineUpdateBuilder();
        update.setProcessors(ImmutableList.of(new ProcessorFactory<>()));
        ingestService.updateIngestPipeline(p, update);
    }

    @Test
    public void createIngestPipeline() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.setDescription("test");
        builder.setProcessors(ImmutableList.of(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ImageIngestor")));
        ingestService.createIngestPipeline(builder);
    }

    @Test(expected = ArchivistException.class)
    public void createIngestPipelineInvalidProcessor() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("test");
        builder.setDescription("test");
        builder.setProcessors(ImmutableList.of(new ProcessorFactory<>()));
        ingestService.createIngestPipeline(builder);
    }

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
