package com.zorroa.common.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.common.AbstractTest;
import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystBuilder;
import com.zorroa.common.domain.AnalystState;
import com.zorroa.common.domain.AnalystUpdateBuilder;
import com.zorroa.sdk.domain.Pager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/28/16.
 */
public class AnalystDaoTests extends AbstractTest {

    @Autowired
    AnalystDao analystDao;

    String id;
    AnalystBuilder builder;

    @Before
    public void init() {
        builder = new AnalystBuilder();
        builder.setState(AnalystState.UP);
        builder.setUrl("http://127.0.0.2:8099");
        builder.setQueueSize(1);
        builder.setMetrics(Maps.newHashMap());
        builder.setArch("osx");
        builder.setStartedTime(System.currentTimeMillis());
        id = analystDao.register("bilbo", builder);
        refreshIndex();
    }

    @Test
    public void testGetAll() {
        assertEquals(1, analystDao.getAll(Pager.first()).size());
    }

    @Test
    public void testRegister() {

        AnalystBuilder builder = new AnalystBuilder();
        builder.setState(AnalystState.UP);
        builder.setUrl("http://127.0.0.2:8099");
        builder.setQueueSize(1);
        builder.setMetrics(Maps.newHashMap());
        builder.setArch("osx");
        builder.setStartedTime(System.currentTimeMillis());

        String id = analystDao.register("charmander", builder);
        refreshIndex();
        Analyst a1 = analystDao.get(id);
        Analyst a2 = analystDao.get(builder.getUrl());
        assertEquals(a1.getId(), a2.getId());
        assertEquals(a1.getUrl(), a2.getUrl());
    }

    @Test
    public void testGetTaskIds() {

        AnalystUpdateBuilder builder = new AnalystUpdateBuilder();
        builder.setState(AnalystState.UP);
        builder.setQueueSize(1);
        builder.setMetrics(Maps.newHashMap());
        builder.setTaskIds(Lists.newArrayList(2, 4, 6, 8));
        builder.setLoad(Lists.newArrayList());
        builder.setThreadsUsed(1);
        analystDao.update(id, builder);
        refreshIndex();

        List<Integer> taskIds = analystDao.getRunningTaskIds();
        assertEquals(taskIds, builder.getTaskIds());
    }
}
