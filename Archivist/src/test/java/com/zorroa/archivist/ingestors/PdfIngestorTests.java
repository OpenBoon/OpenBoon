package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 1/2/16.
 */
public class PdfIngestorTests extends ArchivistApplicationTests {

    @Autowired
    AssetDao assetDao;

    @Autowired
    ObjectFileSystem objectFileSystem;

    @Test
    public void testProcess() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("pdf");
        builder.addToProcessors(
                new ProcessorFactory<>(PdfIngestor.class));
        builder.addToProcessors(
                new ProcessorFactory<>(ProxyProcessor.class));
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(TEST_DATA_PATH + "/office").setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        List<Asset> assets = assetDao.getAll();
        assertEquals(1, assets.size());

        ProxySchema proxies = assets.get(0).getSchema("proxies", ProxySchema.class);
        assertEquals(3, proxies.size());

        assertTrue(objectFileSystem.find("proxies", proxies.get(0).getName()).exists());
    }

    @Test
    public void testSearchByTitle() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("pdf");
        builder.addToProcessors(
                new ProcessorFactory<>(PdfIngestor.class));
        builder.addToProcessors(
                new ProcessorFactory<>(ProxyProcessor.class));
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(TEST_DATA_PATH + "/office").setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        List<Asset> assets = assetDao.getAll();
        logger.info(Json.serializeToString(assets.get(0)));

        assertEquals(1, searchService.search(new AssetSearch().setQuery("pdf")).getHits().totalHits());
    }
}

