package com.zorroa.archivist.repository;

import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.common.repository.AssetDao;
import com.zorroa.common.service.EventLogService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Each test is also testing the getAll function.
 */
public class EventLogDaoTests extends ArchivistApplicationTests {

    @Autowired
    EventLogDao eventLogDao;

    @Autowired
    EventLogService eventLogService;

    @Autowired
    AssetDao assetDao;

    Ingest ingest;
    IngestPipeline pipeline;

    @Before
    public void init() {
        /**
         * So the logging happens right away, otherwise the test exits before the
         * data is applied and we don't see any mapping errors.
         */
        eventLogService.setSynchronous(true);

        pipeline = ingestService.getIngestPipeline("standard");
        IngestBuilder builder = new IngestBuilder(getStaticImagePath());
        ingest = ingestService.createIngest(builder);

        /**
         * Adding a single log will create the alias.
         */
        eventLogService.log(new EventLogMessage("starting event log test"));
        refreshIndex("eventlog", 10);
    }


    @Test
    public void testSimpleLog() {
        long current = eventLogDao.getAll(new EventLogSearch()).getHits().totalHits();
        eventLogService.log(new EventLogMessage("testing {} {} {}", 1, 2, 3));
        refreshIndex("eventlog", 100);

        logger.info("current:{}", current);
        EventLogSearch search = new EventLogSearch();
        assertEquals(current+1, eventLogDao.getAll(search).getHits().totalHits());
    }

    @Test
    public void testLogIngest() {
        eventLogService.log(new EventLogMessage(ingest, "testing {} {} {}", 1, 2, 3));
        refreshIndex("eventlog", 100);

        EventLogSearch search = new EventLogSearch();
        search.setIds(Sets.newHashSet(String.valueOf(ingest.getId())));
        assertEquals(1, eventLogDao.getAll(search).getHits().totalHits());

        search = new EventLogSearch();
        search.setTypes(Sets.newHashSet("Ingest"));
        assertEquals(1, eventLogDao.getAll(search).getHits().totalHits());
    }

    @Test
    public void testLogAsset() {

        AssetBuilder builder = new AssetBuilder(getStaticImagePath() + "/beer_kettle_01.jpg");
        Asset asset = assetDao.upsert(builder);
        refreshIndex();

        eventLogService.log(new EventLogMessage(asset, "testing {} {} {}", 1, 2, 3));
        refreshIndex("eventlog", 100);

        EventLogSearch search = new EventLogSearch();
        search.setIds(Sets.newHashSet(asset.getId()));
        assertEquals(1, eventLogDao.getAll(search).getHits().totalHits());

        search = new EventLogSearch();
        search.setTypes(Sets.newHashSet("Asset"));
        assertEquals(1, eventLogDao.getAll(search).getHits().totalHits());
    }

    @Test
    public void testLog() {
        EventLogMessage message = new EventLogMessage();
        message.setId("1");
        message.setMessage("a log message");
        message.setPath("/foo/bar/bing");
        message.setTags(Sets.newHashSet("foo", "bar"));
        message.setType("Asset");
        eventLogService.log(message);
        refreshIndex("eventlog", 100);

        EventLogSearch search = new EventLogSearch();
        search.setTags(Sets.newHashSet("foo", "bing"));
        assertEquals(1, eventLogDao.getAll(search).getHits().totalHits());
    }

    @Test
    public void testLogException() {
        EventLogMessage message = new EventLogMessage();
        message.setMessage("a log message");
        message.setException(new Exception("foo bar!"));
        eventLogService.log(message);
        refreshIndex("eventlog", 100);

        String[] stack = Json.Mapper.convertValue(
                eventLogDao.getAll().getHits().getHits()[0].getSource().get("stack"), String[].class);
        assertTrue(stack.length > 0);
    }

    @Test
    public void testGetAllEmptySearch() {

        eventLogService.log(new EventLogMessage(ingest, "testing {} {} {}", 1, 2, 3));
        refreshIndex("eventlog", 100);
        assertEquals(2, eventLogDao.getAll(new EventLogSearch()).getHits().totalHits());
    }

    @Test
    public void testGetAllPaged() {
        long current = eventLogDao.getAll(new EventLogSearch()).getHits().totalHits();
        for (int i=0; i<10; i++) {
            eventLogService.log(new EventLogMessage(ingest, "part:{}", i));
        }
        refreshIndex("eventlog", 100);

        /**
         * With 10 entries, a limit of 8 on page 2 should be 2 hits.
         */
        assertEquals(2 + current, eventLogDao.getAll(new EventLogSearch()
                .setPage(2)
                .setLimit(8))
                .getHits().getHits().length);
    }

    @Test
    public void testGetAllWithMessageQueryString() {
        eventLogService.log(new EventLogMessage(ingest, "test jim bob mary {} {} {}", 1, 2, 3));
        eventLogService.log(new EventLogMessage(ingest, "test jesus joseph mary {} {} {}", 1, 2, 3));
        refreshIndex("eventlog", 100);

        assertEquals(1, eventLogDao.getAll(new EventLogSearch("jim")).getHits().totalHits());
        assertEquals(0, eventLogDao.getAll(new EventLogSearch("bob jesus")).getHits().totalHits());
        assertEquals(2, eventLogDao.getAll(new EventLogSearch("bob | mary")).getHits().totalHits());
        assertEquals(2, eventLogDao.getAll(new EventLogSearch("ingest")).getHits().totalHits());
        assertEquals(1, eventLogDao.getAll(new EventLogSearch("ingest bob")).getHits().totalHits());
    }
}
