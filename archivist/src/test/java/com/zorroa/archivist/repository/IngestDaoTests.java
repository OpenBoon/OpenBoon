package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.Schedule;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.processor.ProcessorSpec;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/9/16.
 */
public class IngestDaoTests extends AbstractTest {

    @Autowired
    IngestDao ingestDao;

    Ingest ingest;
    IngestSpec spec;

    @Before
    public void init() {
        spec = new IngestSpec();
        spec.setPipeline(Lists.newArrayList());
        spec.setGenerators(Lists.newArrayList());
        spec.setFolderId(null);
        spec.setPipelineId(null);
        spec.setName("Test");
        spec.setAutomatic(true);
        spec.setRunNow(false);
        spec.setSchedule(new Schedule());
        ingest = ingestDao.create(spec);
    }

    @Test
    public void testCreate() {
        validate(spec, ingest);
    }

    @Test
    public void testUpdate () {
        IngestSpec spec2 = new IngestSpec();
        spec2.setPipeline(Lists.newArrayList(
                new ProcessorSpec().setClassName("foo.Bar")));
        spec2.setGenerators(Lists.newArrayList(
                new ProcessorSpec().setClassName("bing.Bang")));
        spec2.setFolderId(1);
        spec2.setPipelineId(null);
        spec2.setName("Test");
        spec2.setAutomatic(true);
        spec2.setRunNow(false);
        spec2.setSchedule(new Schedule());

        assertTrue(ingestDao.update(ingest.getId(), spec2));
        assertFalse(ingestDao.update(-1, spec2));

        Ingest ingest2 = ingestDao.refresh(ingest);
        validate(spec2, ingest2);
    }

    @Test
    public void testDelete() {
        assertTrue(ingestDao.delete(ingest.getId()));
        assertFalse(ingestDao.delete(ingest.getId()));
    }

    @Test
    public void testGet() {
        Ingest ingest2 = ingestDao.get(ingest.getId());
        validate(ingest, ingest2);
    }

    @Test
    public void testGetByName() {
        Ingest ingest2 = ingestDao.get(ingest.getName());
        validate(ingest, ingest2);
    }

    @Test
    public void testGetAll() {
        List<Ingest> all = ingestDao.getAll();
        validate(ingest, all.get(0));
    }

    @Test
    public void testGetAllPaged() {
        for (int i=0; i<10; i++) {
            spec.setName("Ingest" + i);
            ingestDao.create(spec);
        }
        PagedList<Ingest> list = ingestDao.getAll(Paging.first(5));

        assertEquals(5, list.getList().size());
        assertEquals(11L, (long) list.getPage().getTotalCount());
        assertEquals(3, list.getPage().getTotalPages());
    }

    @Test
    public void testCount() {
        for (int i=0; i<10; i++) {
            spec.setName("Ingest" + i);
            ingestDao.create(spec);
        }
        assertEquals(11, ingestDao.count());
    }

    @Test
    public void testExists() {
        assertTrue(ingestDao.exists("Test"));
        assertFalse(ingestDao.exists("False"));
    }

    public static void validate(Ingest should, Ingest is) {
        assertEquals(should.getPipeline(), is.getPipeline());
        assertEquals(should.getGenerators(), is.getGenerators());
        assertEquals(should.getFolderId(), is.getFolderId());
        assertEquals(should.getPipeline(), is.getPipeline());
        assertEquals(should.getName(), is.getName());
        assertEquals(should.isAutomatic(), is.isAutomatic());
        assertEquals(should.getSchedule(), is.getSchedule());
        assertEquals(should.getId(), is.getId());
    }

    public static void validate( IngestSpec should, Ingest is) {
        assertEquals(should.getPipeline(), is.getPipeline());
        assertEquals(should.getGenerators(), is.getGenerators());
        assertEquals(should.getFolderId(), is.getFolderId());
        assertEquals(should.getPipeline(), is.getPipeline());
        assertEquals(should.getName(), is.getName());
        assertEquals(should.isAutomatic(), is.isAutomatic());
        assertEquals(should.getSchedule(), is.getSchedule());
    }
}
