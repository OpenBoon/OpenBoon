package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.SearchService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * Created by chambers on 1/2/16.
 */
public class VideoIngestorTests extends ArchivistApplicationTests {

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    SearchService searchService;

    @Test
    public void testProcess() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("video");
        builder.addToProcessors(
                new ProcessorFactory<>(VideoIngestor.class));
        builder.addToProcessors(
                new ProcessorFactory<>(ProxyProcessor.class));
        IngestPipeline pipeline = ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(
                new IngestBuilder(TEST_DATA_PATH + "/video").setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        assertEquals(1,
                searchService.search(new AssetSearch().setFilter(
                        new AssetFilter().addToFieldTerms("video.format", "mp4"))).getHits().totalHits());
    }
}


