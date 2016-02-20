/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RetrosheetIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new RetrosheetIngestor()))
                .build();

        File file = getResourceFile("/images/visa.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);
        assertEquals("sunny", asset.getAttr("retrosheet", "sky"));
    }
}
