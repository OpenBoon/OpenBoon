package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 2/17/16.
 */
public class ProxyProcessorTests extends AbstractTest {

    @Test
    public void testProcess() {

        IngestProcessor imageProcessor = initIngestProcessor(new ImageIngestor());
        IngestProcessor proxyProcessor = initIngestProcessor(new ProxyProcessor());

        AssetBuilder builder = new AssetBuilder(new File("src/test/resources/images/toucan.jpg"));
        imageProcessor.process(builder);
        proxyProcessor.process(builder);

        for (Proxy proxy: builder.getSchema("proxies", ProxySchema.class)) {
            assertTrue(new File(proxy.getPath()).isFile());
        }
    }
}
