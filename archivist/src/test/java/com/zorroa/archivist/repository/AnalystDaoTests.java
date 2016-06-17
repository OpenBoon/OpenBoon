package com.zorroa.archivist.repository;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
import com.zorroa.sdk.domain.AnalystState;
import com.zorroa.sdk.domain.AnalystUpdateBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 2/10/16.
 */
public class AnalystDaoTests extends AbstractTest {

    @Autowired
    AnalystDao analystDao;

    @Before
    public void init() {
        AnalystBuilder builder = getAnalystBuilder();
        analystDao.register(builder);
        refreshIndex();
    }

    @Test
    public void testCreate() {
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.1:8099");
        String id = analystDao.register(p);
        Analyst a = analystDao.get(id);
        assertEquals(id, a.getId());
        assertEquals(p.getUrl(), a.getUrl());
    }

    @Test
    public void testUpdate() {
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.1:8099");
        String id = analystDao.register(p);

        AnalystUpdateBuilder u = new AnalystUpdateBuilder();
        u.setQueueSize(100);
        analystDao.update(id, u);

        Analyst a = analystDao.get(id);
        assertEquals(100, a.getQueueSize());

    }

    @Test
    public void testCount() {
        long count = analystDao.count();
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.1:8099");
        analystDao.register(p);
        refreshIndex();

        assertEquals(count+1, analystDao.count());
    }

    @Test
    public void testGet() {
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.1:8099");
        String id = analystDao.register(p);
        Analyst a = analystDao.get(id);
        assertEquals(p.getUrl(), a.getUrl());
    }

    @Test
    public void testGetByUrl() {
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.1:8099");
        String id = analystDao.register(p);
        refreshIndex();
        Analyst a = analystDao.get(p.getUrl());
        assertEquals(p.getUrl(), a.getUrl());
    }

    @Test
    public void testGetAll() {
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.1:8099");
        String id = analystDao.register(p);
        refreshIndex();

        List<Analyst> all = analystDao.getAll(Paging.first());
        assertEquals(2, all.size());
    }

    @Test
    public void testGetActive() {
        AnalystBuilder p = new AnalystBuilder();
        p.setUrl("https://10.0.0.2:8099");
        p.setState(AnalystState.UP);
        analystDao.register(p);
        refreshIndex();

        List<Analyst> all = analystDao.getActive(Paging.first());
        assertEquals(2, all.size());

        p = new AnalystBuilder();
        p.setUrl("https://10.0.0.3:8099");
        p.setState(AnalystState.DOWN);
        analystDao.register(p);
        refreshIndex();

        all = analystDao.getActive(Paging.first());
        assertEquals(2, all.size());
    }
}
