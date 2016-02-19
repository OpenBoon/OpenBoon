package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.client.ExceptionTranslator;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaffeIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {
        IngestProcessor imageProcessor = initIngestProcessor(new ImageIngestor());
        IngestProcessor proxyProcessor = initIngestProcessor(new ProxyProcessor());
        IngestProcessor caffeProcessor = initIngestProcessor(new CaffeIngestor());

        imageProcessor.init();
        proxyProcessor.init();
        caffeProcessor.init();

        AssetBuilder builder = new AssetBuilder(new File("src/test/resources/images/faces.jpg"));
        builder.getSource().setType("image/");
        imageProcessor.process(builder);
        proxyProcessor.process(builder);
        caffeProcessor.process(builder);

        List<String> keywords = builder.getAttr("caffe", "keywords");
        assertEquals(2, keywords.size());
        assertEquals("soccer ball", keywords.get(0));
        assertEquals("swing", keywords.get(1));
    }
}
