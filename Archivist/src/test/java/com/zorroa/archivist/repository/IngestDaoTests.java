package com.zorroa.archivist.repository;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.CreateIngestRequest;
import com.zorroa.archivist.repository.IngestDao;

public class IngestDaoTests extends ArchivistApplicationTests {

    @Autowired
    IngestDao ingestDao;

    CreateIngestRequest request;

    @Before
    public void setup() {
        request = new CreateIngestRequest();
        request.setPaths(Lists.newArrayList(getStaticImagePath()));
    }

    @Test
    public void create() {
        Ingest ingest01 = ingestDao.create(request);
        Ingest ingest02 = ingestDao.get(ingest01.getId());
        assertEquals(ingest02.getId(), ingest02.getId());
    }
}
