package com.zorroa.archivist.repository;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.CreateIngestRequest;
import com.zorroa.archivist.domain.IngestState;
import com.zorroa.archivist.repository.IngestDao;

public class IngestDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestDao ingestDao;

    CreateIngestRequest request;

    @Before
    public void setup() {
        request = new CreateIngestRequest();
        request.setPaths(Lists.newArrayList(getStaticImagePath()));
        request.setFileTypes(Sets.newHashSet());
    }

    @Test
    public void create() {
        Ingest ingest01 = ingestDao.create(request);
        Ingest ingest02 = ingestDao.get(ingest01.getId());
        assertEquals(ingest02.getId(), ingest01.getId());
        assertEquals(ingest02.getPaths(), ingest01.getPaths());
        assertEquals(ingest02.getFileTypes(), ingest01.getFileTypes());
    }

    @Test
    public void getNext() {
        Ingest ingest01 = ingestDao.create(request);
        refreshIndex();

        Ingest ingest02 = ingestDao.getNext();
        assertEquals(ingest02.getId(), ingest01.getId());
        assertEquals(ingest02.getPaths(), ingest01.getPaths());
        assertEquals(ingest02.getFileTypes(), ingest01.getFileTypes());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void setState() {
        Ingest ingest01 = ingestDao.create(request);
        refreshIndex();

        Ingest ingest02 = ingestDao.getNext();
        ingestDao.setState(ingest01, IngestState.RUNNING);
        refreshIndex();

        ingestDao.getNext();
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void start() {
        Ingest ingest01 = ingestDao.create(request);
        refreshIndex();

        Ingest ingest02 = ingestDao.getNext();
        ingestDao.start(ingest01);
        refreshIndex();

        ingestDao.getNext();
    }
}
