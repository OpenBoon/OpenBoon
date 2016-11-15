package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.common.domain.Analyst;
import com.zorroa.common.domain.AnalystBuilder;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 5/20/16.
 */
public class AnalystServiceTests extends AbstractTest {

    AnalystBuilder ping;

    @Before
    public void init() {
        ping = sendAnalystPing();
        refreshIndex();
    }

    @Test
    public void testGetAll() {
        PagedList<Analyst> list = analystService.getAll(Pager.first());
        assertEquals(1, list.size());
    }

    @Test
    public void testGet() {
        Analyst a = analystService.get(ping.getUrl());
        assertEquals(ping.getUrl(), a.getUrl());
    }

    @Test
    public void testGetById() {
        Analyst a1 = analystService.get(ping.getUrl());
        Analyst a2 = analystService.get(a1.getId());
        assertEquals(a1.getUrl(), a2.getUrl());
    }
}
