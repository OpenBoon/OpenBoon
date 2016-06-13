package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by chambers on 2/10/16.
 */
public class AnalystDaoTests extends AbstractTest {

    @Autowired
    AnalystDao analystDao;

    Analyst analyst;

    @Before
    public void init() {
        AnalystPing p = new AnalystPing();
        p.setUrl("https://localhost:8099");
        analyst = analystDao.create(p);
    }

    @Test
    public void testCreate() {
        AnalystPing p = new AnalystPing();
        p.setUrl("https://10.0.0.1:8099");
        assertEquals("https://10.0.0.1:8099", analystDao.create(p).getUrl());
    }

    @Test
    public void testUpdate() {
        AnalystPing p = new AnalystPing();
        p.setUrl("https://10.0.0.1:8099");

        assertFalse(analystDao.update(p));
        analystDao.create(p);
        assertTrue(analystDao.update(p));
    }

    @Test
    public void testCount() {
        assertEquals(1, analystDao.count());

        AnalystPing p = new AnalystPing();
        p.setUrl("https://10.0.0.1:8099");
        assertEquals("https://10.0.0.1:8099", analystDao.create(p).getUrl());
        assertEquals(2, analystDao.count());
    }

    @Test
    public void testGet() {
        Analyst a2 = analystDao.get(analyst.getId());
        assertEquals(analyst, a2);
    }

    @Test
    public void testGetByUrl() {
        Analyst a2 = analystDao.get(analyst.getUrl());
        assertEquals(analyst, a2);
    }

    @Test
    public void testGetAll() {
        int currentCount = jdbc.queryForObject("SELECT COUNT(1) FROM analyst", Integer.class);
        for (int i=0; i<10; i++) {
            AnalystPing p = new AnalystPing();
            p.setUrl("https://10.0.0." + i + ":8099");
            analystDao.create(p);
        }
        assertEquals(currentCount + 10, (int)jdbc.queryForObject("SELECT COUNT(1) FROM analyst", Integer.class));
        assertEquals(currentCount + 10, analystDao.getAll().size());
    }
}
