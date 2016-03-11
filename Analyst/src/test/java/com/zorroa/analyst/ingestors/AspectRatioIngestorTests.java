package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by jbuhler on 2/16/16.
 */
public class AspectRatioIngestorTests  extends AbstractTest {
    @Test
    public void testProcess() throws InterruptedException {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new AspectRatioIngestor()))
                .build();

        File file = getResourceFile("/images/faces.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        String keywords = asset.getAttr("aspect", "keywords");
        assertEquals("landscape", keywords);
    }
}


