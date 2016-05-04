package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.TestHealthIndicator;
import com.zorroa.sdk.domain.*;
import org.elasticsearch.action.count.CountResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/31/15.
 */
public class IngestExecutorServiceTests extends AbstractTest {

    @Autowired
    TestHealthIndicator testHealthIndicator;

    @Test
    public void testUnhealthyIngestExecute() {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipelineId(pipeline.getId()));

        assertTrue(ingestExecutorService.executeIngest(ingest));
        testHealthIndicator.setHealthy(false);
        assertFalse(ingestExecutorService.executeIngest(ingest));
    }

    @Test
    public void testPauseAndResume() {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipelineId(pipeline.getId()));

        /*
         * Set up a timer to resume the ingest.  This is required due to the fact that
         * ingests run in the main thread during unit tests, so executing a paused
         * ingest would block the main thread forever.
         */
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                assertTrue(ingestExecutorService.resume(ingest));
            }
        }, 2000);


        ingestExecutorService.executeIngest(ingest);   // Race condition!
        ingestExecutorService.pause(ingest);
    }

    /**
     * Come back to this when the ignest executor is working.
     */
    @Ignore
    @Test
    public void testIngestCounters() {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("default");

        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);
        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath()).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);

        refreshIndex(100);

        ingest = ingestService.getIngest(ingest.getId());
        assertEquals(2, ingest.getCreatedCount());
        assertEquals(0, ingest.getUpdatedCount());
        assertEquals(0, ingest.getErrorCount());

        refreshIndex(100);

        ingestExecutorService.executeIngest(ingest);
        ingest = ingestService.getIngest(ingest.getId());
        assertEquals(0, ingest.getCreatedCount());
        assertEquals(2, ingest.getUpdatedCount());
        assertEquals(0, ingest.getErrorCount());
    }

    String[] monthName = { "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December" };

    @Test
    public void testIngestAggregators() throws InterruptedException {

        Ingest ingest = addTestAssets("agg");
        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        String month = monthName[Calendar.getInstance().get(Calendar.MONTH)];

        // Validate date folders
        Folder yearFolder = folderService.get("/Date/" + year);
        assertNotEquals(null, yearFolder);
        List<Folder> yearChildren = folderService.getChildren(yearFolder);
        assertEquals(1, yearChildren.size());
        Folder monthFolder = folderService.get("/Date/" + year + "/" + month);
        assertNotEquals(null, monthFolder);

        AssetSearch search = new AssetSearch().setFilter(new AssetFilter().setFolderId(monthFolder.getId()));
        CountResponse response = searchService.count(search);
        assertEquals(3, response.getCount());

        // Validate star rating folders
        Folder ratingFolder = folderService.get("/★ Rating/★★★★");
        assertNotEquals(null, ratingFolder);
        search = new AssetSearch().setFilter(new AssetFilter().setFolderId(ratingFolder.getId()));
        response = searchService.count(search);
        assertEquals(3, response.getCount());

        // Validate ingest path folders
        Folder ingestFolder = ingestService.getFolder(ingest);
        logger.info("folder: {}", ingestFolder);
        assertNotEquals(null, ingestFolder);
        List<Folder> children = folderService.getChildren(ingestFolder);
        assertEquals(1, children.size());
        Folder childFolder = children.get(0);
        assertEquals("child", childFolder.getName());
        search = new AssetSearch().setFilter(new AssetFilter().setFolderId(childFolder.getId()));
        response = searchService.count(search);
        assertEquals(2, response.getCount());
        children = folderService.getChildren(childFolder);
        assertEquals(1, children.size());
        Folder grandkidFolder = children.get(0);
        assertEquals("grandkid", grandkidFolder.getName());
        search = new AssetSearch().setFilter(new AssetFilter().setFolderId(grandkidFolder.getId()));
        response = searchService.count(search);
        assertEquals(1, response.getCount());
    }
}
