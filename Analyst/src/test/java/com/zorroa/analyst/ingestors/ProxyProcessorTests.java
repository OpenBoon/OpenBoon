package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.domain.Proxy;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ProxySchema;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 2/17/16.
 */
public class ProxyProcessorTests extends AbstractTest {

    @Test
    public void testProcess() {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .build();

        File file = getResourceFile("/images/toucan.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        for (Proxy proxy: asset.getSchema("proxies", ProxySchema.class)) {
            assertTrue(new File(proxy.getPath()).isFile());
        }
    }

    @Test
    public void testArgs() {
        ProxyProcessor proxyProcessor = new ProxyProcessor();

        // Construct a map with the parsed JSON test data
        Map<String, Object> proxyOutput = ImmutableMap.<String, Object>builder()
                .put("size", 227)
                .put("bpp", 8)
                .put("format", "png")
                .put("quality", 0.5)
                .build();
        List<Map<String, Object>> proxyOutputs = ImmutableList.<Map<String, Object>>builder()
                .add(proxyOutput)
                .build();
        Map<String, Object> args = ImmutableMap.<String, Object>builder()
                .put("proxies", proxyOutputs)
                .build();

        // Set the arguments and call init to extract arguments into instance
        proxyProcessor.setArgs(args);
        proxyProcessor.init();

        // Test to make sure extraction worked
        List<ProxyProcessor.Output> outputs = proxyProcessor.getOutputs();
        assertEquals(1, outputs.size());
        assertEquals("png", outputs.get(0).format);
        assertEquals(227, outputs.get(0).size);
        assertEquals(8, outputs.get(0).bpp);
        assertEquals("quality", 0.5, outputs.get(0).quality, 1e-6);
    }
}
