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

import static org.junit.Assert.assertEquals;

/**
 * Note: we currently only create the mapping in Archivist.
 */
public class AnalystServiceTests extends AbstractTest {

    @Autowired
    AnalyzeService analyzeService;

    @Test
    public void testAnalyze() {
        AnalyzeRequest req = new AnalyzeRequest();
        req.setUser("test");
        req.setIngestId(1);
        req.setIngestPipelineId(1);
        req.setProcessors(Lists.newArrayList(new ProcessorFactory<>(ImageIngestor.class)));
        req.setPaths(Lists.newArrayList(new File("src/test/resources/images/toucan.jpg").getAbsolutePath()));

        AnalyzeResult result = analyzeService.analyze(req);
        assertEquals(result.created, 1);
    }
}
