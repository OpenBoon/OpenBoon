package com.zorroa.archivist.repository;

import static org.junit.Assert.assertEquals;

import org.elasticsearch.client.Client;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
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
        assertEquals(ingest01.getNewAssetCount(), ingest.getNewAssetCount());
        assertEquals(ingest01.getPath(), ingest.getPath());
        assertEquals(ingest01.getPipelineId(), ingest.getPipelineId());
        assertEquals(ingest01.getState(), ingest.getState());
        assertEquals(ingest01.getTimeCreated(), ingest.getTimeCreated());
        assertEquals(ingest01.getTimeModified(), ingest.getTimeModified());
        assertEquals(ingest01.getTimeStarted(), ingest.getTimeStarted());
        assertEquals(ingest01.getTimeStopped(), ingest.getTimeStopped());
        assertEquals(ingest01.getFileTypes(), ingest.getFileTypes());
        assertEquals(ingest01.getUpdatedAssetCount(), ingest.getUpdatedAssetCount());
        assertEquals(ingest01.getUserCreated(), ingest.getUserCreated());
        assertEquals(ingest01.getUserModified(), ingest.getUserModified());
    }
}
