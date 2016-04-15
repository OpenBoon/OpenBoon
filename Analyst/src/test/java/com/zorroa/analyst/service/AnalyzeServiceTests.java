package com.zorroa.analyst.service;

import com.google.common.collect.Lists;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.analyst.ingestors.ImageIngestor;
import com.zorroa.archivist.sdk.domain.AnalyzeRequest;
import com.zorroa.archivist.sdk.domain.AnalyzeResult;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

/**
 * Note: we currently only create the mapping in Archivist.
 */
public class AnalyzeServiceTests extends AbstractTest {

    @Autowired
    AnalyzeService analyzeService;

    @Test
    public void testAnalyze() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(ImageIngestor.class)));
        req.addToAssets(new File("src/test/resources/images/toucan.jpg"));

        AnalyzeResult result = analyzeService.analyze(req);
        assertEquals(1, result.created);
    }

    @Test
    public void testIgnoredUnsupportedPaths() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(ImageIngestor.class)));
        req.addToAssets(new File("src/test/resources/images/README.md"));

        AnalyzeResult result = analyzeService.analyze(req);
        assertEquals(result.created, 0);
        assertEquals(result.updated, 0);
        assertEquals(result.tried, 0);
    }

    @Test(expected=ExecutionException.class)
    public void testAnalyzeInitFailure() throws ExecutionException {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(new Random().nextInt(9999));
        req.setIngestPipelineId(new Random().nextInt(9999));
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>("com.foo.Bar")));
        req.addToAssets(new File("src/test/resources/images/toucan.jpg"));

        AnalyzeResult result = analyzeService.asyncAnalyze(req);
        assertEquals(result.created, 1);
    }
}
