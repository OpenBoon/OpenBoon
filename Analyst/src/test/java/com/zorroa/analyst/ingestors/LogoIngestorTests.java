package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class LogoIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        IngestProcessor imageProcessor = initIngestProcessor(new ImageIngestor());
        IngestProcessor proxyProcessor = initIngestProcessor(new ProxyProcessor());
        IngestProcessor logoProcessor = initIngestProcessor(new LogoIngestor());

        imageProcessor.init();
        proxyProcessor.init();
        logoProcessor.init();

        AssetBuilder builder = new AssetBuilder(new File("src/test/resources/images/visa12.jpg"));
        imageProcessor.process(builder);
        proxyProcessor.process(builder);
        builder.getSource().setType("image/");
        logoProcessor.process(builder);

        List<String> keywords = builder.getAttr("Logos", "keywords");
        assertEquals(2, keywords.size());
        assertEquals("visa", keywords.get(0));
        assertEquals("visa0.5", keywords.get(1));
    }

}
