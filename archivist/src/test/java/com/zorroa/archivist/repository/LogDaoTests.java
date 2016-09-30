package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.archivist.service.LogService;
import com.zorroa.sdk.domain.Pager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by chambers on 8/31/16.
 */
public class LogDaoTests extends AbstractTest {


    @Autowired
    LogService logService;

    @Before
    public void init() throws InterruptedException {
        logService.log(new LogSpec().setAction("test").setMessage("A test message"));
        Thread.sleep(1100);
        refreshIndex();
    }

    @Test
    public void testSearch() {
        LogSearch search = new LogSearch();
        search.setAggs(ImmutableMap.of("all",
                ImmutableMap.of("global", ImmutableMap.of(),
                        "aggs", ImmutableMap.of("actions",
                                ImmutableMap.of("terms",ImmutableMap.of("field", "action"))))));

        logService.search(search, Pager.first());
    }

}
