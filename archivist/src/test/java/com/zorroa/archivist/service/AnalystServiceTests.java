package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystBuilder;
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
    }

    @Test
    public void testGet() {
        Analyst a = analystService.get(ping.getUrl());
        assertEquals(ping.getUrl(), a.getUrl());
    }
}
