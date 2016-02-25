package com.zorroa.archivist.sdk.client.analyst;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the analyst client.  Must have an analyst running on local host.
 */
public class AnalystClientIntegrationTests {

    private static final Logger logger = LoggerFactory.getLogger(AnalystClientIntegrationTests.class);

    AnalystClient analyst = new AnalystClient("https://127.0.0.1:8099");
    String path = new File("src/test/resources/images/beer_kettle_01.jpg").getAbsolutePath();

    @Test(expected=IngestException.class)
    public void testAnalyzeClassNotFound() {
        AnalyzeResult result = analyst.analyze(new AnalyzeRequest()
                .setIngestId(1)
                .setIngestPipelineId(1)
                .setPaths(Lists.newArrayList(path))
                .setProcessors(Lists.newArrayList(new ProcessorFactory<>("com.foo.bar.Nope"))));
        assertEquals(0, result.tried);
    }

    @Test
    public void testAnalyze() {
        AnalyzeResult result = analyst.analyze(new AnalyzeRequest()
                .setIngestId(1)
                .setIngestPipelineId(1)
                .setPaths(Lists.newArrayList(path))
                .setProcessors(Lists.newArrayList(new ProcessorFactory<>("com.zorroa.analyst.ingestors.ImageIngestor"))));
        assertEquals(1, result.tried);
        assertEquals(1, result.created + result.updated);
    }
}
