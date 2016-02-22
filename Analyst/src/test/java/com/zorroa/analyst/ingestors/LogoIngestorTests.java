package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LogoIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .add(initIngestProcessor(new LogoIngestor()))
                .build();

        File file = getResourceFile("/images/visa12.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        List<String> keywords = asset.getAttr("Logos", "keywords");
        assertEquals(3, keywords.size());
        assertEquals("visa", keywords.get(0));
        assertEquals("bigvisa", keywords.get(1));
        assertEquals("visa0.6283255086071987", keywords.get(2));
    }

}
