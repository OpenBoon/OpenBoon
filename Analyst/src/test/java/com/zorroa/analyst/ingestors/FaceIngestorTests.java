package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class FaceIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        IngestProcessor imageProcessor = initIngestProcessor(new ImageIngestor());
        IngestProcessor proxyProcessor = initIngestProcessor(new ProxyProcessor());
        IngestProcessor faceProcessor = initIngestProcessor(new FaceIngestor());

        imageProcessor.init();
        proxyProcessor.init();
        faceProcessor.init();

        AssetBuilder builder = new AssetBuilder(new File("src/test/resources/images/faces.jpg"));
        imageProcessor.process(builder);
        proxyProcessor.process(builder);
        builder.getSource().setType("image/");
        faceProcessor.process(builder);

        List<String> keywords = builder.getAttr("face", "keywords");
        assertEquals(3, keywords.size());
        assertEquals("face", keywords.get(0));
        assertEquals("face1", keywords.get(1));
        assertEquals("face0.5", keywords.get(2));
    }
}
