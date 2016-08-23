package com.zorroa.common.repository;

import com.google.common.collect.Maps;
import com.zorroa.common.AbstractTest;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
import com.zorroa.sdk.domain.AnalystState;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 7/28/16.
 */
public class AnalystDaoTests extends AbstractTest {

    @Autowired
    AnalystDao analystDao;

    @Test
    public void testRegister() {

        AnalystBuilder builder = new AnalystBuilder();
        builder.setState(AnalystState.UP);
        builder.setUrl("http://127.0.0.2:8099");
        builder.setQueueSize(1);
        builder.setMetrics(Maps.newHashMap());
        builder.setArch("osx");
        builder.setStartedTime(System.currentTimeMillis());

        String id = analystDao.register(builder);
        refreshIndex();
        Analyst a1 = analystDao.get(id);
        Analyst a2 = analystDao.get(builder.getUrl());
        assertEquals(a1.getId(), a2.getId());
        assertEquals(a1.getUrl(), a2.getUrl());
    }
}
