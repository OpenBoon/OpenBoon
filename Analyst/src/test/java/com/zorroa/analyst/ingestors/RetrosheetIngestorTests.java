/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.analyst.ingestors;

import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class RetrosheetIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        IngestProcessor imageProcessor = initIngestProcessor(new ImageIngestor());
        IngestProcessor retroProcessor = initIngestProcessor(new RetrosheetIngestor());
        imageProcessor.init();
        retroProcessor.init();
        AssetBuilder builder = new AssetBuilder(new File("src/test/resources/images/visa.jpg"));
        builder.getSource().setType("image/");
        imageProcessor.process(builder);
        retroProcessor.process(builder);
        assertEquals("sunny", builder.getAttr("retrosheet", "sky"));
    }
}
