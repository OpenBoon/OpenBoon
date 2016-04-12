package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

/**
 * Created by barbara on 1/10/16.
 */
public class ColorAnalysisIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .add(initIngestProcessor(new ColorAnalysisIngestor()))
                .build();

        File file = getResourceFile("/images/faces.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);
        Map<int[], Float> analysisResults = asset.getAttr("color.clusters");
        Map<int[], String> colorMapping = asset.getAttr("color.mapping");
        assertNotEquals(analysisResults, null);
        assertNotEquals(colorMapping, null);
    }
}
