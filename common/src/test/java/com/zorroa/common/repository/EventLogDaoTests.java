package com.zorroa.common.repository;

import com.google.common.collect.Sets;
import com.zorroa.common.AbstractTest;
import com.zorroa.common.domain.EventLoggable;
import com.zorroa.common.domain.EventSearch;
import com.zorroa.common.domain.EventSpec;
import com.zorroa.common.domain.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Each test is also testing the getAll function.
 */
public class EventLogDaoTests extends AbstractTest {

    @Autowired
    EventLogDao eventLogDao;

    @Before
    public void init() {
        /**
         * So the logging happens right away, otherwise the test exits before the
         * data is applied and we don't see any mapping errors.
         */
        eventLogDao.setSynchronous(true);

        /**
         * Adding a single log will create the alias.
         */
        eventLogDao.info(EventSpec.log("starting event log test"));
        refreshIndex();
    }

    @Test
    public void testSimpleLog() {
        long current = eventLogDao.getAll(new EventSearch()).getPage().getTotalCount();
        eventLogDao.info(EventSpec.log("testing {} {} {}", 1, 2, 3));
        refreshIndex();

        EventSearch search = new EventSearch();
        assertEquals(current+1, eventLogDao.getAll(search).size());
    }

    @Test
    public void testCount() {
        long current = eventLogDao.getAll(new EventSearch()).getPage().getTotalCount();
        eventLogDao.info(EventSpec.log("testing {} {} {}", 1, 2, 3));
        refreshIndex();

        EventSearch search = new EventSearch();
        assertEquals(current+1, eventLogDao.count(search));
    }

    @Test
    public void testLogEventLoggable() {
        EventLoggable object = new EventLoggable() {
            @Override
            public String getEventLogId() {
                return "1";
            }

            @Override
            public String getEventLogType() {
                return "EventLoggable";
            }
        };

        eventLogDao.info(EventSpec.log(object, "testing {} {} {}", 1, 2, 3));
        refreshIndex();

        EventSearch search = new EventSearch();
        search.setObjectIds(Sets.newHashSet("1"));
        assertEquals(1, eventLogDao.getAll(search).size());

        search = new EventSearch();
        search.setObjectTypes(Sets.newHashSet("EventLoggable"));
        assertEquals(1, eventLogDao.getAll(search).size());
    }


    @Test
    public void testLogException() {
        eventLogDao.info(EventSpec.log(() -> "1", new Exception("foo bar!"), "testing {} {} {}", 1, 2, 3));
        refreshIndex();
        assertNotNull(eventLogDao.getAll(Paging.first()).get(0).getStack());
    }
}
