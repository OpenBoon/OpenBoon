package com.zorroa.archivist.processors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.SearchService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 1/2/16.
 */
public class PdfIngestorTests extends ArchivistApplicationTests {
    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    AssetDao assetDao;

    @Autowired
    SearchService searchService;

    @Test
    public void testProcess() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("pdf");
        builder.addToProcessors(
                new ProcessorFactory<>(PdfIngestor.class));
        builder.addToProcessors(
                new ProcessorFactory<>(ProxyProcessor.class));
        IngestPipeline pipeline = ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(TEST_DATA_PATH).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        List<Asset> assets = assetDao.getAll();
        assertEquals(1, assets.size());

        ProxySchema proxies = assets.get(0).getSchema("proxies", ProxySchema.class);
        assertEquals(3, proxies.size());
        assertTrue(new File(proxies.get(0).getPath()).exists());
    }

    @Test
    public void testSearchByTitle() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("pdf");
        builder.addToProcessors(
                new ProcessorFactory<>(PdfIngestor.class));
        builder.addToProcessors(
                new ProcessorFactory<>(ProxyProcessor.class));
        IngestPipeline pipeline = ingestPipelineDao.create(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(TEST_DATA_PATH).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        List<Asset> assets = assetDao.getAll();
        logger.info(Json.serializeToString(assets.get(0)));

        assertEquals(1, searchService.search(new AssetSearch().setQuery("Microsoft")).getHits().totalHits());
    }
}

