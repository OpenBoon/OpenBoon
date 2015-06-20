package com.zorroa.archivist.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestState;
import com.zorroa.archivist.domain.ProxyConfig;
import com.zorroa.archivist.service.ImageService;
import com.zorroa.archivist.service.IngestService;

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

    Client elasticClient;

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
    public void testGetPending() {
        List<Ingest> pending = ingestDao.getPending();
        assertEquals(1, pending.size());

        IngestBuilder builder = new IngestBuilder(getStaticImagePath());
        ingestDao.create(pipeline, proxyConfig, builder);

        pending = ingestDao.getPending();
        assertEquals(2, pending.size());

        ingestDao.setState(ingest, IngestState.Finished);
        pending = ingestDao.getPending();
        assertEquals(1, pending.size());

    }

    @Test
    public void testSetState() {
        ingestDao.setState(ingest, IngestState.Finished);
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(ingest01.getState(), IngestState.Finished);
    }


    @Test
    public void testSetRunning() {
        assertTrue(ingestDao.setRunning(ingest));
        assertFalse(ingestDao.setRunning(ingest));
    }

    @Test
    public void testSetFinished() {
        assertFalse(ingestDao.setFinished(ingest));
        assertTrue(ingestDao.setRunning(ingest));
        assertTrue(ingestDao.setFinished(ingest));
    }

    @Test
    public void testIncrementCreatedCount() {
        ingestDao.incrementCreatedCount(ingest, 10);
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(10, ingest01.getCreatedCount());
    }

    @Test
    public void testIncrementErrorCount() {
        ingestDao.incrementErrorCount(ingest, 10);
        Ingest ingest01 = ingestDao.get(ingest.getId());
        assertEquals(10, ingest01.getErrorCount());
    }

}
