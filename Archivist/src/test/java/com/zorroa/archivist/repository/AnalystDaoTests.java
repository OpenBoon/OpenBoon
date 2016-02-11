package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.Analyst;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.*;

/**
 * Created by chambers on 2/10/16.
 */
public class AnalystDaoTests extends ArchivistApplicationTests {

    @Autowired
    AnalystDao analystDao;

    Analyst analyst;

    @Before
    public void init() {
        AnalystPing p = new AnalystPing();
        p.setHost("localhost");
        analyst = analystDao.create(p);
    }

    @Test
    public void testCreate() {
        AnalystPing p = new AnalystPing();
        p.setHost("test");
        assertEquals("test", analystDao.create(p).getHost());
    }

    @Test
    public void testUpdate() {
        AnalystPing p = new AnalystPing();
        p.setHost("foo");

        assertFalse(analystDao.update(p));
        analystDao.create(p);
        assertTrue(analystDao.update(p));
    }

    @Test
    public void testGet() {
        Analyst a2 = analystDao.get(analyst.getId());
        assertEquals(analyst, a2);
    }

    @Test
    public void testGetAll() {
        for (int i=0; i<10; i++) {
            AnalystPing p = new AnalystPing();
            p.setHost("test" + i);
            analystDao.create(p);
        }
        assertEquals(11, (int)jdbc.queryForObject("SELECT COUNT(1) FROM analyst", Integer.class));
        assertEquals(11, analystDao.getAll().size());
    }
}
