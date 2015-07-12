package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.ImageService;
import com.zorroa.archivist.service.IngestService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

public class IngestDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestDao ingestDao;

    @Autowired
    IngestService ingestService;


    @Autowired
    ImageService imageService;

    Ingest ingest;
    IngestPipeline pipeline;
    ProxyConfig proxyConfig;

    @Before
    public void init() {
        pipeline = ingestService.getIngestPipeline("standard");
        proxyConfig = imageService.getProxyConfig("standard");
        IngestBuilder builder = new IngestBuilder(getStaticImagePath());
        ingest = ingestDao.create(pipeline, proxyConfig, builder);
    }

    @Test
    public void testCreate() {
        IngestBuilder builder = new IngestBuilder(getStaticImagePath());
        Ingest ingest01 = ingestDao.create(pipeline, proxyConfig, builder);
        Ingest ingest02 = ingestDao.get(ingest01.getId());
        assertEquals(ingest01.getId(), ingest02.getId());
    }

    @Test
    public void testGet() {
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(ingest01.getId(), ingest.getId());
        assertEquals(ingest01.getPath(), ingest.getPath());
        assertEquals(ingest01.getPipelineId(), ingest.getPipelineId());
        assertEquals(ingest01.getState(), ingest.getState());
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
        updateBuilder.setFileTypes(Sets.newHashSet("jpg"));
        updateBuilder.setPath("/foo");
        updateBuilder.setPipelineId(testPipeline.getId());
        updateBuilder.setProxyConfigId(testProxyConfig.getId());

        assertTrue(ingestDao.update(ingest, updateBuilder));

        Ingest updatedIngest = ingestDao.get(ingest.getId());
        assertEquals("/foo", updatedIngest.getPath());
        assertTrue(updatedIngest.getFileTypes().contains("jpg"));
        assertEquals(1, updatedIngest.getFileTypes().size());
        assertEquals(testPipeline.getId(), updatedIngest.getPipelineId());
        assertEquals(testProxyConfig.getId(), updatedIngest.getProxyConfigId());
    }

}
