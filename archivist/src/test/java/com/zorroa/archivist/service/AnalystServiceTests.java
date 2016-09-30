package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
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
    String id = "f4f4fad5-7870-574d-9f84-ce52aeaa2229";
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
        Analyst a = analystService.get(id);
        assertEquals(ping.getUrl(), a.getUrl());
    }
}
