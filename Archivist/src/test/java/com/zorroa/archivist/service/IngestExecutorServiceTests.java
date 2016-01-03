package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.IngestService;
import org.elasticsearch.action.count.CountResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/31/15.
 */
public class IngestExecutorServiceTests extends ArchivistApplicationTests {


    @Autowired
    IngestExecutorService ingestShedulerService;

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    SearchService searchService;

    @Autowired
    FolderService folderService;

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

        ingestExecutorService.executeIngest(ingest);
        ingest = ingestService.getIngest(ingest.getId());
        assertEquals(0, ingest.getCreatedCount());
        assertEquals(2, ingest.getUpdatedCount());
        assertEquals(0, ingest.getErrorCount());
    }

    @Test
    public void testIngestAggregators() throws InterruptedException {

        Ingest ingest = ingestService.createIngest(new IngestBuilder(getStaticImagePath("agg")));
        ingestExecutorService.executeIngest(ingest);

        refreshIndex();

        // Validate date folders
        Folder yearFolder = folderService.get("/Date/2014");
        assertNotEquals(null, yearFolder);
        List<Folder> yearChildren = folderService.getChildren(yearFolder);
        assertEquals(1, yearChildren.size());
        Folder monthFolder = folderService.get("/Date/2014/October");
        assertNotEquals(null, monthFolder);
        monthFolder = folderService.get("/Date/2015/February");
        assertNotEquals(null, monthFolder);
        AssetSearch search = new AssetSearch().setFilter(new AssetFilter().setFolderId(monthFolder.getId()));
        CountResponse response = searchService.count(search);
        assertEquals(1, response.getCount());

        // Validate star rating folders
        Folder ratingFolder = folderService.get("/★ Rating/★★★★");
        assertNotEquals(null, ratingFolder);
        search = new AssetSearch().setFilter(new AssetFilter().setFolderId(ratingFolder.getId()));
        response = searchService.count(search);
        assertEquals(1, response.getCount());

        // Validate ingest path folders
        Folder ingestFolder = folderService.get("/Ingests");
        assertNotEquals(null, ingestFolder);
        List<Folder> children = folderService.getChildren(ingestFolder);
        assertEquals(1, children.size());
        Folder aggFolder = children.get(0);
        children = folderService.getChildren(aggFolder);
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
