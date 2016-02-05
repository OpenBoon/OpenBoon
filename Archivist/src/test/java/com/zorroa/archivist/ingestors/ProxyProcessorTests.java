package com.zorroa.archivist.ingestors;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProxyProcessorTests extends ArchivistApplicationTests {

    @Autowired
    AssetDao assetDao;

    @Test
    public void testProcess() throws InterruptedException {

        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setName("proxy");
        builder.addToProcessors(
                new ProcessorFactory<>(ImageIngestor.class));
        builder.addToProcessors(
                new ProcessorFactory<>(ProxyProcessor.class));
        IngestPipeline pipeline = ingestService.createIngestPipeline(builder);

        Ingest ingest = ingestService.createIngest(new IngestBuilder(
                getStaticImagePath("standard")).setPipelineId(pipeline.getId()));
        ingestExecutorService.executeIngest(ingest);
        refreshIndex();

        Asset asset = assetDao.getAll().get(0);
        assertTrue(asset.contains("tinyProxy"));
        assertTrue(asset.contains("proxies"));
        ProxySchema proxies = asset.getSchema("proxies", ProxySchema.class);

        int[] widths = new int[] { 128, 256, 1024 };
        for (int i=0; i<widths.length; i++) {
            Proxy p = proxies.get(i);
            assertEquals(p.getWidth(), widths[i]);
        }
    }

}
