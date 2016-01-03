package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.domain.IngestScheduleBuilder;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.service.ImageService;
import com.zorroa.archivist.sdk.service.IngestService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.util.List;

import static org.junit.Assert.*;

public class IngestDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestDao ingestDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    ImageService imageService;

    @Autowired
    IngestScheduleDao ingestScheduleDao;

    Ingest ingest;
    IngestPipeline pipeline;

    @Before
    public void init() {
        pipeline = ingestService.getIngestPipeline("standard");
        IngestBuilder builder = new IngestBuilder(getStaticImagePath());
        ingest = ingestDao.create(pipeline, builder);
    }

    @Test
    public void testCreate() {
        IngestBuilder builder = new IngestBuilder(getStaticImagePath());
        Ingest ingest01 = ingestDao.create(pipeline, builder);
        Ingest ingest02 = ingestDao.get(ingest01.getId());
        assertEquals(ingest01.getId(), ingest02.getId());
    }

    @Test
    public void testDelete() {
        assertTrue(ingestDao.delete(ingest));
        assertFalse(ingestDao.delete(ingest));
    }

    @Test
    public void testGet() {
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(ingest01.getId(), ingest.getId());
        assertEquals(ingest01.getPath(), ingest.getPath());
        assertEquals(ingest01.getPipelineId(), ingest.getPipelineId());
        assertEquals(ingest01.getState(), ingest.getState());
        assertEquals(ingest01.getAssetWorkerThreads(), ingest.getAssetWorkerThreads());
        assertEquals(ingest01.getTimeCreated(), ingest.getTimeCreated());
        assertEquals(ingest01.getTimeModified(), ingest.getTimeModified());
        assertEquals(ingest01.getTimeStarted(), ingest.getTimeStarted());
        assertEquals(ingest01.getTimeStopped(), ingest.getTimeStopped());
        assertEquals(ingest01.getFileTypes(), ingest.getFileTypes());
        assertEquals(ingest01.getUserCreated(), ingest.getUserCreated());
        assertEquals(ingest01.getUserModified(), ingest.getUserModified());
    }

    @Test
    public void testGetAll() {
        List<Ingest> pending = ingestDao.getAll();
        assertEquals(1, pending.size());
    }

    @Test
    public void testGetAllBySchedule() {
        IngestScheduleBuilder builder = new IngestScheduleBuilder();
        builder.setDays(Lists.newArrayList(DayOfWeek.FRIDAY));
        builder.setRunAtTime("10:00:00");
        builder.setName("10AM");
        builder.setIngestIds(Lists.newArrayList(ingest.getId()));

        IngestSchedule schedule = ingestScheduleDao.create(builder);
        logger.info("{}", schedule.getIngestIds());

        assertTrue(ingestDao.getAll(schedule).contains(ingest));
        assertEquals(1, ingestDao.getAll(schedule).size());
    }

    @Test
    public void testGetByFilter() {
        // non existent pipelines
        IngestFilter filter = new IngestFilter();
        filter.setPipelines(Lists.newArrayList("foo", "bar"));
        List<Ingest> ingests = ingestDao.getAll(filter);
        assertEquals(0, ingests.size());

        // existing pipeline
        filter = new IngestFilter();
        filter.setPipelines(Lists.newArrayList(pipeline.getName()));
        ingests = ingestDao.getAll(filter);
        assertEquals(1, ingests.size());
    }

    @Test
    public void testGetAllByState() {
        List<Ingest> idle = ingestDao.getAll(IngestState.Idle, 1000);
        assertEquals(1, idle.size());
        assertTrue(ingestDao.setState(ingest, IngestState.Running, IngestState.Idle));
        idle = ingestDao.getAll(IngestState.Idle, 1000);
        assertEquals(0, idle.size());
    }

    @Test
    public void testSetStateWithOldState() {
        assertTrue(ingestDao.setState(ingest, IngestState.Running, IngestState.Idle));
        assertFalse(ingestDao.setState(ingest, IngestState.Running, IngestState.Idle));
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(ingest01.getState(), IngestState.Running);
    }

    @Test
    public void testSetState() {
        assertTrue(ingestDao.setState(ingest, IngestState.Running));
        assertFalse(ingestDao.setState(ingest, IngestState.Running));
    }

    @Test
    public void testUpdate() {

        IngestPipelineBuilder ipb = new IngestPipelineBuilder();
        ipb.setName("test");
        ipb.setDescription("A test pipeline");
        ipb.addToProcessors(new ProcessorFactory<>("com.zorroa.archivist.processors.ChecksumProcessor"));
        IngestPipeline testPipeline = ingestService.createIngestPipeline(ipb);

        IngestUpdateBuilder updateBuilder = new IngestUpdateBuilder();
        updateBuilder.setFileTypes(Sets.newHashSet("jpg"));
        updateBuilder.setPath("/foo");
        updateBuilder.setPipelineId(testPipeline.getId());
        updateBuilder.setAssetWorkerThreads(6);

        assertTrue(ingestDao.update(ingest, updateBuilder));

        Ingest updatedIngest = ingestDao.get(ingest.getId());
        assertEquals("/foo", updatedIngest.getPath());
        assertTrue(updatedIngest.getFileTypes().contains("jpg"));
        assertEquals(1, updatedIngest.getFileTypes().size());
        assertEquals(updatedIngest.getAssetWorkerThreads(), 6);
        assertEquals(testPipeline.getId(), updatedIngest.getPipelineId());
    }

    @Test
    public void testCounters() {
        ingestDao.updateCounters(ingest, 1, 2, 3);
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(1, ingest01.getCreatedCount());
        assertEquals(2, ingest01.getUpdatedCount());
        assertEquals(3, ingest01.getErrorCount());
        ingestDao.resetCounters(ingest);
        ingest01 = ingestDao.get(ingest.getId());
        assertEquals(0, ingest01.getCreatedCount());
        assertEquals(0, ingest01.getUpdatedCount());
        assertEquals(0, ingest01.getErrorCount());
    }

}
